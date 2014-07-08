(defproject vaulted/vaulted-clj "0.1.3-SNAPSHOT"
  :description "Clojure bindings for Vaulted API."
  :url "http://www.vaulted.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [environ "0.4.0"]
                 [clj-http "0.9.1"]
                 [cheshire "5.3.1"]
                 [slingshot "0.10.3"]
                 [prismatic/schema "0.2.1"]]
  :repl-init vaulted-clj.core)
