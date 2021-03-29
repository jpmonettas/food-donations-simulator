(ns fd-sim.main
  (:require [reagent.dom :as rdom]
            [fd-sim.views :as views]
            [fd-sim.subs]
            [fd-sim.events]))

(defn ^:dev/after-load mount-component []
  (println "Mounting fd-sim.views/main over #app")
  (rdom/render [views/main]
               (.getElementById js/document "app")))

(defn ^:export init []
  (mount-component))
