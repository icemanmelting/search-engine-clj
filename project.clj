(defproject search-engine-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [aero "1.1.2"]
                 [log4j/apache-log4j-extras "1.2.17"]
                 [org.clojure/tools.logging "0.4.0"]
                 [http-kit "2.2.0"]
                 [compojure "1.6.1" :exclusions [clj-time]]
                 [prismatic/schema "1.1.3"]
                 [ring/ring-json "0.4.0" :exclusions [clj-time]]
                 [ring-cors "0.1.9"]
                 [org.postgresql/postgresql "42.1.4"]
                 [com.layerware/hugsql "0.4.8"]]
  :main search-engine-clj.core
  :uberjar-name "search-engine-standalone.jar"
  :profiles {:uberjar {:aot :all}})
