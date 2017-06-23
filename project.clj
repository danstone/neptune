(defproject neptune "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.3.443"]
                 [org.clojure/clojurescript "1.9.562"]
                 [org.clojure/tools.analyzer "0.6.9"]
                 [org.clojure/tools.analyzer.jvm "0.6.10"]
                 [http-kit "2.2.0"]
                 [ring/ring-core "1.5.1"]
                 [eval-soup "1.2.1"]
                 [paren-soup "2.8.6"]
                 [reagent "0.6.2"]
                 [secretary "1.2.3"]
                 [reagent-forms "0.5.29"]]
  :plugins [[lein-cljsbuild "1.1.6"]]
  :source-paths ["src/clj"]
  :cljsbuild
  {:builds [{:id "dev"
             :source-paths ["src/cljs"]
             :figwheel true
             :compiler {:main "neptune.client.core"
                        :output-to "resources/neptune/js/main.js"
                        :asset-path "js/out"}}]}
  :profiles {:dev {:source-paths ["script" "dev"]
                   :dependencies [[figwheel-sidecar "0.5.10"]]}})
