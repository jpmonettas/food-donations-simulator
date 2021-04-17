(ns fd-sim.main
  (:require [reagent.dom :as rdom]
            [re-frame.core :as re-frame]
            [fd-sim.views :as views]
            [fd-sim.subs]
            [fd-sim.events]
            [flow-storm.api :as fsa]))

(defn ^:dev/after-load mount-component []
  (println "Mounting fd-sim.views/main over #app")
  (rdom/render [views/main]
               (.getElementById js/document "app")))

(defn ^:export init []
  #_(fsa/connect {:tap-name "fd-sim"})
  #_(fsa/trace-ref re-frame.db/app-db {:ref-name "re-frame-db"
                                     ;; comment out this to debug donation-explorer
                                     ;; this is ignored so map dragging events don't spam flow-storm                                     
                                     :ignore-keys [:collector/donation-explorer]})
  (re-frame/dispatch-sync [:collector/initialize])
  (mount-component))
