(defproject puppetlabs/crates-caching-proxy "0.1.0-SNAPSHOT"
  :description "Clojure web application that proxies and caches Rust Cargo crates from crates.io"
  :url "http://github.com/puppetlabs/crates-caching-proxy"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring "1.5.0-RC1"]
                 [puppetlabs/http-client "0.5.0"]
                 [puppetlabs/comidi "0.3.1"]
                 [puppetlabs/kitchensink "1.3.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [slingshot "0.12.2"]
                 [ch.qos.logback/logback-classic "1.1.7"]
                 [environ "1.0.3"]]
  :profiles {:dev {:dependencies [[ring/ring-mock "0.3.0"]]}}
  :aliases {"crate-proxy" ["trampoline" "run" "-m" "crates-caching-proxy.core"]})

