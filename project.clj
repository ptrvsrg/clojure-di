(defproject clojure-di "0.0.0"
  :description "A Clojure library for DI container"
  :url "https://github.com/ptrvsrg/clojure-di"
  :repositories [["github" {:url      "https://maven.pkg.github.com/ptrvsrg/clojure-di"
                            :username "private-token"
                            :password :env/CI_RW_TOKEN}]]
  :license {:name "Apache 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.12.4"]]
  :plugins [[lein-cloverage/lein-cloverage "1.2.4"]
            [lein-set-version "0.4.1"]
            [com.github.clj-kondo/lein-clj-kondo "0.2.5"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "1.1.1"]]}}
  :repl-options {:init-ns clojure-di.core})
