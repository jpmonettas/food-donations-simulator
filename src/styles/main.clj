(ns styles.main
  (:require [garden.stylesheet :refer [at-media at-keyframes]]))

(def screen
  [:body {:font-family "sans-serif"
          }
   [:.main {}
    [:.top-bar {:display :flex
                :flex-direction :row
                :justify-content :space-between}]]])

(def tabs
  [[:.tabs {:display :flex
            :cursor :pointer
            :flex-direction :row
            ;;:justify-content :space-between
;;            :min-width "250px"
            }
    [:.tab {:display :inline-block
            :border "1px solid #333"
            :padding "5px"}
     [:&.selected {:color :red}]]]

   [:.tab-content {:padding-top "10px"}
    [:div {:margin-bottom "10px"}]]])

(def consumers
  [[:img.profile {:width "50px" :height "50px"}]])

(def panel
  [[:.panel {:border "1px solid #333"
             :padding "5px"}
    [:h2 {:margin "0px"}]
    [:.title {:margin-bottom "10px"}]
    ]])

(def tables
  [[:table {:font-size "13px"
            :border-collapse :collapse
            :width "100%"}
    [:th {:text-align :left}]
    [:td {}]]])

;; This creates resources/public/css/main.css
(def ^:garden main
  (list
   screen
   tabs
   consumers
   panel
   tables
   ))


