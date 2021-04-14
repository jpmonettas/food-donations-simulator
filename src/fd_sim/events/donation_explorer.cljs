(ns fd-sim.events.donation-explorer)
(def max-scale 22)
(def min-scale 0.1)

(defn screen-coord->proj-coord
  "Convert from:
       - screen-coord: [x,y]      coordinates in screen pixels, 0 <= x <= map-width, 0 <= y <= map-height
   into
       - proj-coord:   [x,y]      coordinates in map projection coords, 0 <= x <= 360, 0 <= y <= 180
   `translate`: current map translation
   `scale`: current map scale   
  "
  [translate scale [screen-x screen-y]]
  
  (let [[tx ty] translate]
    [(/ (- screen-x tx) scale)
     (/ (- screen-y ty) scale)]))

(defn select-donation [db [_ donation-id]]
  (-> db
      (assoc-in [:ui/selected-tab :main] :transparency)
      (assoc-in [:ui/selected-tab :transparency] :donation-explorer)
      (assoc-in [:collector/donation-explorer :selected-donation] donation-id)
      (assoc-in [:collector/donation-explorer :map-state] {:scale 0.2
                                                           :translate [0 0]})))

(defn calculate-scale-factor [scale]
  (if (< scale 1.5)
    0.1
    0.8))

(defn zoom [db [_ {:keys [delta x y]}]]
  (let [screen-coords [x y]
        map-state (get-in db [:collector/donation-explorer :map-state])
        {:keys [translate scale]} map-state
        zoom-dir (if (pos? delta) -1 1)
        [proj-x proj-y] (screen-coord->proj-coord translate scale screen-coords)]
    (update-in db [:collector/donation-explorer :map-state]
               (fn [{:keys [translate scale] :as map-state}]
                 (let [scale-factor (calculate-scale-factor scale)
                       new-scale (+ scale (* zoom-dir scale-factor))
                       scaled-proj-x (* proj-x scale)
                       scaled-proj-y (* proj-y scale)
                       new-scaled-proj-x (* proj-x new-scale)
                       new-scaled-proj-y (* proj-y new-scale)
                       x-scale-diff (- scaled-proj-x new-scaled-proj-x)
                       y-scale-diff (- scaled-proj-y new-scaled-proj-y)
                       [tx ty] translate
                       new-translate-x (+ tx x-scale-diff)
                       new-translate-y (+ ty y-scale-diff)]
                   
                   (if (< min-scale new-scale max-scale)
                     (assoc map-state
                            :translate [(int new-translate-x) (int new-translate-y)]
                            :scale new-scale)
                     map-state))))))

(defn map-grab [db [_ {:keys [x y]}]]
  (-> db
      (assoc-in [:collector/donation-explorer :map-state :grab :screen-current]  [x y])))

(defn map-grab-release [db _]
  (update-in db [:collector/donation-explorer :map-state] dissoc :grab))

(defn drag [db [_ {:keys [x y]}]]
  (let [map-state (get-in db [:collector/donation-explorer :map-state]) 
        current-screen-coord [x y]]
    (if (:grab map-state)
      (let [[screen-x screen-y] current-screen-coord
            before-screen-coord (-> map-state :grab :screen-current)
            [before-screen-x before-screen-y] before-screen-coord
            screen-drag-x (- screen-x before-screen-x)
            screen-drag-y (- screen-y before-screen-y)]
        (-> db            
            (assoc-in  [:collector/donation-explorer :map-state :grab :screen-current] current-screen-coord)
            (update-in [:collector/donation-explorer :map-state :translate 0] + screen-drag-x)
            (update-in [:collector/donation-explorer :map-state :translate 1] + screen-drag-y)))
      db)))
