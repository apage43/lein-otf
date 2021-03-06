(ns lein-otf.plugin
  (:require [clojure.java.io :as io]
            [robert.hooke :as rh]
            [leiningen.uberjar]))

(defn juggle
  "Put in a loader namespace for :main and put the real main namespace in a
manifest field. The lein-otf loader should already be present as a dependency."
  [main project]
  (if-let [real (or main (:main project))]
    (-> project
        (dissoc :main)
        (assoc-in [:manifest "lein-otf-real-main"] (str real))
        (assoc-in [:manifest "Main-Class"] "lein_otf.loader.Stub"))
    project))

(defn hook-uberjar-otf
  "Build an uberjar with on-the-fly (OTF) Clojure compilation."
  [uberjar project & [main :as args]]
  (let [juggle* (partial juggle main)
        new-meta (update-in (meta project) [:without-profiles] juggle*)
        project (with-meta (juggle* project) new-meta)]
    (apply uberjar project args)))

;; Called by Leiningen when plugin is loaded.
(defn hooks
  "Apply hook to modify project map when building uberjar."
  [] (rh/add-hook #'leiningen.uberjar/uberjar hook-uberjar-otf))

;; Used by Leiningen to modify projects
(defn middleware
  "Middleware to inject lein-otf.loader as a dependency."
  [project]
  (-> project
      (update-in [:dependencies] conj ['org.timmc/lein-otf.loader "1.0.1"])
      (update-in [:aliases] assoc "uberjar-otf" "uberjar")))
