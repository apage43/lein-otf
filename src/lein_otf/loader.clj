(ns lein-otf.loader
  "Loader to be added to projects as a sacrifice to the AOT gods."
  (:require clojure.main)
  (:import (java.net URL JarURLConnection)
           (java.util.jar Manifest Attributes))
  (:gen-class))

(defn- ^Manifest get-manifest
  "Retrieve the manifest out of the current JAR file. This is tricky, since it
is easy to accidentally pick up a MANIFEST.MF out of some other jar on the
class path if you use .getResourceAsStream."
  []
  (-> ^Class (class get-manifest) ;; get a class file definitely in *this* jar
      ^URL (.getResource "/lein_otf/loader.class") ;; any resource will do
      ^JarURLConnection (.openConnection)
      (.getManifest)))

(defn- get-real
  "Get the real namespace from a custom attribute on the jar manifest."
  []
  {:post [(string? %)]}
  (let [manifest (get-manifest)]
    (-> manifest
        ^Attributes (.getMainAttributes)
        (.getValue "lein-otf-real-main"))))

(defn -main
  "Loader entrance point; just relays the call on to the real -main."
  [& args]
  (let [real-ns (get-real)]
    ;; clojure.main does not have the -m option in Clojure 1.2, so this is
    ;; taken directly from the 1.3 clojure.main
    (apply (ns-resolve (doto (symbol real-ns) (require)) '-main) args)))