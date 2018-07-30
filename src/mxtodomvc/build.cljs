(ns mxtodomvc.build
  (:require
    [tiltontec.util.core :as util]

    [tiltontec.cell.core :refer-macros [cF cF+ cFn cF+n cFonce cF1] :refer [cI]]
    [tiltontec.model.core :refer [<mget mset!>] :as md]
    [mxweb.gen
     :refer-macros [div section header h1 footer p]]))

;; Below we demonstrate the HTML work-alike quality of mxWeb, and that
;; CLJS can be coded naturally as needed.
;;
;; A nice win of everything just being CLJS is that we get "Web Components"
;; for free. This first "component" function is rather simple, but of course
;; could take as many parameters as needed to be as reusable as needed.

(defn mxtodo-credits []
  (footer {:class "info"}
    ;;
    ;; yes, we can mix CLJS with the pseudo-HTML functions,
    ;; simply because in mxWeb everythig is CLJS;
    ;;
    ;; here "children" will be a single vector. We could also
    ;; toss in another child and/or a nil and/or a nested vector.
    ;; It all gets flattened with nils pruned.
    ;;
    (for [credit ["Double-click a to-do list item to edit it."
                  "Created by <a href=\"https://github.com/kennytilton\">Kenneth Tilton</a>."
                  "Inspired by <a href=\"https://github.com/tastejs/todomvc/blob/master/app-spec.md\">TodoMVC</a>."]]
      (p credit))))

;(defn wall-clock [interval detail]
;  (let [steps (atom 100)]
;    (div {:class   "std-clock"
;          :content (cF (subs (.toTimeString
;                               (js/Date.
;                                 (<mget me :clock)))
;                         0 9))}
;      {:clock  (cI (util/now))
;       :ticker (cFonce (js/setInterval
;                         #(when (pos? (swap! steps dec))
;                            (let [time-step 1000
;                                  w (<mget me :clock)]
;                              (mset!> me :clock (+ w time-step))))
;                         interval))})))

;(defn wall-clock [interval detail]
;  (let [steps (atom 100)]
;    (div {:class   "std-clock"}
;      {:clock  (cI (util/now))
;       :ticker (cFonce (js/setInterval
;                         #(when (pos? (swap! steps dec))
;                            (let [time-step 1000
;                                  w (<mget me :clock)]
;                              (mset!> me :clock (+ w time-step))))
;                         interval))}
;      ;; and now the simple string content for the div...
;      (do
;        (prn "clock")
;        (subs (->> (<mget me :clock)
;              (js/Date.)
;              (.toTimeString)) 0 9)))))

(defn wall-clock [interval detail]
  (div {:class "std-clock"}
    {:clock  (cI (util/now))
     :ticker (cFonce
               ;; cFonce provides the timer function access to
               ;; the anaphoric "me" (aka this aka self) so
               ;; it can feed the app matrix
               (js/setInterval
                 #(mset!> me :clock (util/now))
                 interval))}
    ;; and now the simple string content for the div...
    (subs (->> (<mget me :clock)
            (js/Date.)
            (.toTimeString)) 0 detail)))

(defn matrix-build []
  (md/make
    ;;
    ;; HTML tag syntax is (<tag> [dom-attribute-map [custom-property map] children*]
    ;;
    :mx-dom (section {:class "todoapp"}
              (wall-clock 1000 9)
              (header {:class "header"}
                (h1 "todos")
                (mxtodo-credits)))))