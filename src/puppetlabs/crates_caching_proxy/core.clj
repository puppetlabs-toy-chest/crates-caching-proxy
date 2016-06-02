(ns puppetlabs.crates-caching-proxy.core
  (:import [java.nio.file Files Paths StandardCopyOption Path CopyOption]
           [java.nio.file.attribute FileAttribute])
  (:require [ring.adapter.jetty :as rj]
            [clojure.string :as str]
            [puppetlabs.http.client.sync :as hsync]
            [ring.util.request :as rreq]
            [ring.util.response :as rresp]
            [puppetlabs.comidi :as cmdi]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [throw+ try+]]
            [environ.core :as environ]))

(def cache-root (delay (get environ/env :cache-root "proxy-cache")))

(def port (delay (Long/parseLong (get environ/env :crate-proxy-port "8080"))))

(def crates-io-api-root "/api/v1/crates")

(def crates-io-url (str "https://crates.io" crates-io-api-root))

(defn ^Path path-get [^String s & more-strings]
  (Paths/get s (into-array String more-strings)))

(defn ^Path create-temp-file [^Path dir prefix suffix]
  (Files/createTempFile dir prefix suffix (into-array FileAttribute [])))

(defn create-file-response [package version package-name]
  (rresp/file-response (format "%s/%s/%s" package version package-name)
                       {:root @cache-root
                        :index-files? false
                        :allow-symlinks? false}))

(defn download-crate [package version]
  
  (let [package-url (format "%s/%s/%s/download" crates-io-url package version)
        _ (log/infof "Downloading package using URL '%s'" package-url)
        resp (hsync/get package-url)]

    (log/debugf "Response received from URL '%s' was '%s' with a content length of"
                package-url
                (:status resp)
                (get-in resp [:headers "content-length"]))

    (if (= 200 (:status resp))
      (:body resp)
      (throw+ {:kind ::download-error
               :status (:status resp)
               :response-body (slurp (:body resp))}
              (format "Error downloading package '%s'" package-url)))))

(defn store-crate [package version package-name]
  (let [crate-path (path-get @cache-root package version)
        final-crate-path (path-get @cache-root package version package-name)]
    (Files/createDirectories crate-path (into-array FileAttribute []))
    (let [temp-file (create-temp-file crate-path package "crate")]
      (with-open [input-stream (download-crate package version)]
        (Files/copy input-stream temp-file (into-array CopyOption [StandardCopyOption/REPLACE_EXISTING])))
      (Files/move temp-file final-crate-path (into-array CopyOption [StandardCopyOption/ATOMIC_MOVE]))
      (create-file-response package version package-name))))

(defn find-crate [package version package-name]
  (if-let [file-response (create-file-response package version package-name)]
    (do
      (log/infof "Found version '%s' of '%s' cached, returning cached version" version package-name)
      file-response)
    (do
      (log/infof "Version '%s' of '%s' cached, not found locally, downloading it" version package-name)
      (store-crate package version package-name))))

(def cache-handler
  (cmdi/routes->handler
   (cmdi/context crates-io-api-root
                 (cmdi/routes
                  (cmdi/GET ["/" :package "/" :version "/download"] [package version]
                            (rresp/redirect
                             (format "%s/%s/%s/%s.crate" crates-io-api-root package version package)))
                  (cmdi/GET ["/" :package-dir "/" :version "/" :package-name] [package-dir version package-name]
                            (try+
                             (find-crate package-dir version package-name)
                             (catch [:kind ::download-error] {:keys [status response-body]}
                               (log/errorf "Error occured while downloading version '%s' of '%s'. Status was '%s' with response '%s"
                                           version package-name status response-body)
                               {:status status
                                :body (format (str "Downloading remote packaged failed."
                                                   "Error from remote host was '%s%")
                                              response-body)})
                             (catch Exception e
                               (log/errorf e "Error occured while downloading version '%s' of '%s'"
                                           version package-name)
                               {:status 500
                                :body (.getMessage e)})))))))

(defn -main []
  (let [startup-log-str (format "Starting the caching proxy with '%s' as the proxy's cache directory, on port '%s'"
                                @cache-root @port)]
    (println startup-log-str)
    (log/info startup-log-str))
  (rj/run-jetty #'cache-handler {:port @port}))

