(ns io.pietermaes.wellplatedesign.client
  (:require
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
   [com.fulcrologic.fulcro.dom :as dom]
   [com.fulcrologic.fulcro.mutations :refer [defmutation]]
   ["/clingo" :as clingo]))

(def c
  (clingo #js {:locateFile (fn [name]
                             "clingo.wasm")}))

(defn run
  [clingo asp opts]
  (.ccall clingo "run" "number" #js ["string", "string"] #js [asp opts]))

(.then c (fn [c]
           (run c "sudoku(1..100)." "--outf=2")))

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

(def default-well-size 96)

(defn rows+columns-for-size
  [size]
  (if-let [[nrow ncol] (well-plate-sizes size)]
    [(take nrow rows) (take ncol columns)]
    (throw (ex-info "Unknown well size" {:size size
                                         :acceptable (keys well-plate-sizes)}))))

(defn well-plate [{:plate/keys [id size]}]
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

(defn set-plate-size*
  [state-map size]
  (-> state-map
      (assoc-in [:options :options/size] size)))

(defmutation set-plate-size
  [{:keys [size]}]
  (action [{:keys [state]}]
          (swap! state set-plate-size* size)))

(defsc Options [this {:options/keys [size]}]
  {:query [:options/size]
   :initial-state (fn [_] {:options/size default-well-size})}
  (dom/select {:value (str size)
               :onChange #(comp/transact! this [(set-plate-size {:size (int (.. % -target -value))})])}
              (for [wp-size (keys well-plate-sizes)]
                (dom/option {:value (str wp-size)}
                            wp-size))))

(def ui-options (comp/factory Options))

(defsc Root [this {:keys [options] :as props}]
  {:query [{:options (comp/get-query Options)}]
   :initial-state (fn [_] {:options (comp/get-initial-state Options {})})}
  (dom/div (tw "bg-gray-100" "min-h-screen" "flex")
           (dom/div (tw "w-2/3" "flex")
                    (dom/div (tw "m-auto")
                             (well-plate {:plate/size (:options/size options)})))
           (dom/div (tw "w-1/3" "bg-gray-300")
                    (ui-options options))))

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
