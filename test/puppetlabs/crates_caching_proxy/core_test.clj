(ns puppetlabs.crates-caching-proxy.core-test
  (:import [java.nio.file Files Paths Path]
           [java.nio.file.attribute FileAttribute]
           [java.io ByteArrayInputStream])
  (:require [clojure.test :refer :all]
            [puppetlabs.crates-caching-proxy.core :refer :all]
            [ring.mock.request :as mock]
            [clojure.java.io :as io]
            [puppetlabs.http.client.sync :as hsync]))

(defn str-bytes [s]
  (.getBytes s "UTF-8"))

(deftest test-download-redirect
  (let [resp (cache-handler (mock/request :get "/api/v1/crates/hyper/0.8.0/download"))]
    (is (= 302 (:status resp)))
    (is (= "/api/v1/crates/hyper/0.8.0/hyper.crate"
           (get-in resp [:headers "Location"])))
    (is (= "" (:body resp)))))

(deftest test-crate-storage
  (let [temp-path (Files/createTempDirectory (path-get "target")
                                             "test-crate-storage"
                                             (into-array FileAttribute []))]
    (testing "basic crate storage and cache hits"
      (with-redefs [cache-root (delay (-> temp-path
                                          .toAbsolutePath
                                          (str "/my-cache1")))
                    hsync/get (fn [url]
                                {:status 200
                                 :body (java.io.ByteArrayInputStream. (str-bytes "file contents"))})]

        (is (false? (.exists (io/file @cache-root))))
        (let [resp (cache-handler (mock/request :get "/api/v1/crates/hyper/0.8.0/hyper.crate"))]
          (is (= 200 (:status resp)))
          (is (= "file contents" (slurp (:body resp))))
          (is (true? (.exists (io/file @cache-root))))
          (is (true? (.exists (io/file (str @cache-root "/hyper/0.8.0/hyper.crate"))))))

        ;; Rebinding the get function as it should be pulling from local cache, not re-downloading the crate
        (with-redefs [hsync/get (fn [url]
                                  (throw (RuntimeException. "Should retrieve from cache")))]
          (let [resp (cache-handler (mock/request :get "/api/v1/crates/hyper/0.8.0/hyper.crate"))]
            (is (= 200 (:status resp)))
            (is (= "file contents" (slurp (:body resp))))))))

    (testing "failures to download from crates.io"
      (with-redefs [cache-root (delay (-> temp-path
                                          .toAbsolutePath
                                          (str "/my-cache2")))
                    hsync/get (fn [url]
                                {:status 404
                                 :body (ByteArrayInputStream. (str-bytes "Not Found"))})]

        (is (false? (.exists (io/file @cache-root))))
        (let [resp (cache-handler (mock/request :get "/api/v1/crates/hyper/0.8.0/hyper.crate"))]
          (is (= 404 (:status resp)))
          (is (re-find #"Not Found" (:body resp))))))))
