(ns io.pietermaes.wellplatedesign.client
  (:require
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
   [com.fulcrologic.fulcro.dom :as dom]))

(defn- collate
  [m x]
  (cond
    (string? x) (update m :classes conj x)
    (map? x) (merge m x)
    :else m))

(defn tw
  [& args]
  (reduce collate {} args))

(defonce app (app/fulcro-app))

(def rows "ABCDEFGHIJKLMNOPQRSTUVWXYZ")

(def columns (mapv str (range 1 25)))

(def well-plate-sizes {6 [2 3]
                       12 [3 4]
                       24 [4 6]
                       48 [6 8]
                       96 [8 12]
                       384 [16 24]})

(defn rows+columns-for-size
  [size]
  (if-let [[nrow ncol] (well-plate-sizes size)]
    [(take nrow rows) (take ncol columns)]
    (throw (ex-info "Unknown well size" {:size size
                                         :acceptable (keys well-plate-sizes)}))))

(defsc WellPlate [this {:plate/keys [id size]}]
  {:query [:plate/id :plate/size]
   :initial-state (fn [{:keys [size] :as params}] {:plate/id (tempid/tempid)
                                                   :plate/size size})
   :ident [:plate/by-id :plate/id]}
  (let [[rows columns] (rows+columns-for-size size)
        padding 8
        radius 10
        font-size 14
        column-label-padding 13
        row-label-padding 13
        nrows (count rows)
        ncols (count columns)
        diameter (* 2 radius)
        column-label-height (* font-size 2)
        row-label-width (* font-size 2)
        space-between-midpoints (+ diameter padding)
        width (+ (* ncols space-between-midpoints)
                 row-label-width)
        height (+ (* nrows space-between-midpoints)
                  column-label-height)
        start-x (+ row-label-width row-label-padding)
        start-y (+ column-label-height column-label-padding)
        x-midpoints (range start-x width space-between-midpoints)
        y-midpoints (range start-y height space-between-midpoints)
        label (fn [x y text row-or-column]
                (dom/text {:x x :y y
                           :style {:textAnchor "middle"
                                   :dominantBaseline "central"
                                   :fontSize font-size
                                   :fill "#4a5568"}}
                          text))]
    (dom/div
     (dom/svg {:width width :height height}
              (map (fn [cx col]
                     (label cx font-size col :column))
                   x-midpoints columns)
              (map (fn [cy row]
                     (label font-size cy row :row))
                   y-midpoints rows)
              (for [[cx col] (map vector x-midpoints columns)
                    [cy row] (map vector y-midpoints rows)]
                (dom/circle {:cx cx
                             :cy cy
                             :r radius
                             :fill "white"
                             :stroke "#cbd5e0"}))))))

(def ui-well-plate (comp/factory WellPlate))

(defsc Root [this {:keys [plate] :as props}]
  {:query [{:plate (comp/get-query WellPlate)}]
   :initial-state (fn [_] {:plate (comp/get-initial-state WellPlate {:size 96})})}
  (dom/div (tw "bg-gray-100" "min-h-screen" "flex")
           (dom/div (tw "w-2/3" "flex")
                    (dom/div (tw "m-auto")
                             (ui-well-plate plate)))
           (dom/div (tw "w-1/3" "bg-gray-300" "shadow-lg")
                    "Sidebar")))

(defn ^:export init
  "Shadow-cljs sets this up to be our entry-point function. See shadow-cljs.edn `:init-fn` in the modules of the main build."
  []
  (app/mount! app Root "app")
  (js/console.log "Loaded"))

(defn ^:export refresh
  "During development, shadow-cljs will call this on every hot reload of source. See shadow-cljs.edn"
  []
  ;; re-mounting will cause forced UI refresh, update internals, etc.
  (app/mount! app Root "app")
  (js/console.log "Hot reload"))
