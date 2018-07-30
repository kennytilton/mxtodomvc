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

(defn wall-clock [mode interval start end]
  (div {:class "std-clock"}

    ;; Here we introduce custom properties for the widget. The popular term
    ;; for keeping state alongside the widget most involved with that state
    ;; is "co-location". Was "together" was too easy? At any rate, the idea
    ;; is that the HTML stand-ins can have a life outside the DOM, if you
    ;; will. Our state ends up distributed across the application.
    ;;
    ;; We also introduce one of the pillars of Matrix: "lifting" an
    ;; existing component that knows nothing about the Matrix, in this
    ;; case the system clock, into the Matrix with whatever glue it takes.
    ;;
    ;; The system clock lift here is superficial and requires just a few lines
    ;; of code, but the essence is achieved: the view now enjoys dataflow
    ;; from the system clock.
    ;;
    ;; One final point: we illustrate a parameterized component, the wall-clock.
    ;;
    ;; Glossary:
    ;;    cI     -- make a Matrix input cell initialized with the value shown;
    ;;    cFonce -- a formulaic cell that runs just once, for Matrix
    ;;              lifecycle reasons we will visit when needed; and
    ;;    mset!> -- procedural, imperative assignment to an input cell.

    {:clock  (cI (util/now))
     :ticker (cFonce
               ;; cFonce provides the timer function access to
               ;; the anaphoric "me" (aka this aka self) so
               ;; it can feed the app matrix.
               ;;
               ;; This is a lifecycle thing: cell formulas run after
               ;; a model instance has been created, so they have access
               ;; to the instance.
               ;;
               (js/setInterval
                 #(mset!> me :clock (util/now))
                 interval))}

    ;; and now the simple string content as the one and only
    ;; child element of the div...

    (as-> (<mget me :clock) date
      (js/Date. date)
      (case mode
        :time (.toTimeString date)
        :date (.toDateString date))

      (subs date start end))
    ))

(defn matrix-build []
  (md/make
    ;;
    ;; HTML tag syntax is (<tag> [dom-attribute-map [custom-property map] children*]
    ;;
    :mx-dom (section {:class "todoapp"}
              (wall-clock :date 60000 0 15)
              (wall-clock :time 1000 0 8)
              (header {:class "header"}
                (h1 "todos?")
                (mxtodo-credits)))))