# TodoMVC, with Matrix Inside&trade;
*An introduction by example to Matrix dataflow and mxWeb*

mxWeb&trade; makes web pages easier to build, debug, and revise simply by changing what happens when we read and write properties:
* when B reads A, A remembers B; and
* when we write to A, A tells B.

Mysterious, right? But those are just the essentials. As we expand on them, their expression as a different way of programming the web will be less surprising. We begin by providing concrete examples of each. 

#### B reads A
What does it mean for B to read A? It means B is expressed as an HLL function that reads A. Colloquially, we call these *formulas*.
````clojure
(li
    {:class (cF (when (<mget todo :completed)
                  "completed"))}
    ...)
````
The above is an excerpt from TodoMVC, which we will build in the [next write-up](documentation/BuildingTodoMVC.md). Without getting too much into the weeds, `li` makes a proxy LI instance. Its API mirrors the HTML syntax `<li attribute*> children* </li>`. `cF` (for *formulaic cell*) makes `:class` functional. 

`<mget` is the Matrix property reader that remembers which property is asking. We can hide the `<mget` noise with a pithier wrapper:

````clojure
(defn td-completed [todo]
  (<mget todo :completed))
````

Back to our app. In the next excerpt, the Matrix manages a `to-do` model property, "model" as in MVC. Note `td-deleted`, hiding an `<mget` while still establishing a dependency on the `:deleted` property of each `to-do`. 
````clojure
(md/make ::todo-list
    :items-raw (cI nil)
    :items (cF (remove td-deleted (<mget me :items-raw)))
    :empty? (cF (empty? (<mget me :items))))
````
`cI` sets that property up to tell functional  properties `:items` when `:items-raw` changes. Functional `:items` will tell functional `:empty?` if *it* has changed. `me` is like `self` or `this`, so in this case would be the `todo-list`.

Aside: those simple derivations could just as well be ordinary functions of the to-do list, but these are just two small carveouts in the progressive decomposition of TodoMVC. Our win will be the aggregate decomposition, not the size of any particular carveout.

#### A tells B
What does it mean for A to tell B? It means that, when we imperatively change A, Matrix internals will automatically recalculate B:
````cljs
(input {:class       "toggle"
        ::mxweb/type "checkbox"
        :onclick     #(mswap!> todo :completed
                         #(when-not [%] (util/now)})
````
The Matrix property writer `mswap!>`:
* changes the `:completed` property of the model todo; and
* *before returning* recomputes the :class property of the proxy `li` we saw above.

### Digging deeper
A few more fundamentals:
* on-change handlers, or "observers", may be supplied for A or B; and
* we might have a property K for "kids", such as the children of a parent DOM element.

Again, some concrete examples...
#### on-change handlers, or "observers"
In the example above, the `:class` property of a proxy `li` instance gained or lost the "completed" string as the user toggled the model to-do's `:completed` property via an `onclick` handler. Great, but how does the actual DOM `li` classlist get changed?

Changing properties manifest themselves via on-change callbacks we call *observers*. When `A` changes, an observer can:
* mutate properties outside the Matrix graph; or
* enqueue Matrix writes to other properties for execution immediately after the current write.

The mxWeb library provides an observer for maintaining the DOM:
````clojure
(defmethod observe-by-type
  [:mxweb.base/tag]
  [property model new-value old-value cell]
  (let [dom (tag-dom model)]
    (case property
       ...others...
       :class (classlist/set dom new-value))))
````
mxWeb proxy instances know which DOM element they represent, and because Matrix tracks change by property we have no need for VDOM or diffing: mxWeb knows exactly what to change.

Notes:
* we offer no example of a deferred write at this time.  
Those arise when applications have grown quite large, when the *developer* decides some observed property change demands a change at the application semantic level, as if the user were making a change.
* *caveat lectorum* we use "observer" in the strict dictionary sense: "monitor, not participant". Other libraries use "observer" for what we call dependent or formulaic properties.

#### K for Kids
Formulas can compute more than just descriptive properties such as "completed". We might have `K` for "kids" holding the children of some parent, such as the `LI` nodes under a `UL` DOM list. In other words, the population of our application model can grow or shrink with events. 

We call a dynamic population of causally connected models a *matrix*.

> ma·trix ˈmātriks *noun* an environment in which something else takes form. *Origin:* Latin, female animal used for breeding, parent plant, from *matr-*, *mater*

Here is how our TodoMVC will avoid rebuilding the full DOM list of to-dos when: (1) a to-do is added or deleted; (2) the user selects a different filter; or (3) the `:completed` property of a to-do is changed. 
````clojure
(ul {:class "todo-list"}
  {:kid-values  (cF (sort-by td-created
                      (<mget (mx-todos me)
                        (case (<mget (mx-find-matrix mx) :route)
                          "All" :items
                          "Completed" :items-completed
                          "Active" :items-active))))
   :kid-key     #(<mget % :todo)
   :kid-factory (fn [me todo]
                  (todo-list-item todo))}
  ;; cache is prior value for this implicit ':kids' slot; k-v-k uses it for diffing
  (kid-values-kids me cache))
````
As an exercise, try pairing  the `<mget` dependencies above with the enumerated ways the list can change. The one not evident -- changes to the completed property of a todo -- is expressed by the collections `:items` *et al* we saw defined above.

### Extending the scope: lifting
We explained above how the computed `:class` "completed" got propagated to the actual DOM classlist by an observer. That hints at the next fundamental, which we call "lifting". 

The DOM knows nothing about Matrix, so we developed sufficient "glue" code to make it seem as if it did. mxWeb consists of six hundred lines of code creating two classes (one for HTML tags, one for CSS Styles) and other code to translate HLL handlers into native handlers. 

In the full implementation of TodoMVC we will see even more systems lifted into the Matrix: routing, XHR, localStorage RSN), and even a few lines to lift the system clock.  

### Related work
> "Derived Values, Flowing" -- the [re-frame](https://github.com/Day8/re-frame/blob/master/README.md) tag-line

Matrix enjoys much good company in this field. Other recommended CLJS libraries are [Reagent](https://reagent-project.github.io/), [Hoplon/Javelin](https://github.com/hoplon/javelin), and [re-frame](https://github.com/Day8/re-frame). Beyond CLJS, we admire [MobX](https://github.com/mobxjs/mobx/blob/master/README.md) (JS), [binding.Scala](https://github.com/ThoughtWorksInc/Binding.scala/blob/11.0.x/README.md), and Python [Trellis](https://pypi.org/project/Trellis/). Please let us know about any we missed.

### Really?
Can we really program this way? This 80KLOC [Algebra intelligent tutor](https://tiltonsalgebra.com/#) consists of about twelve hundred `A`s and `B`s. Everything runs under Matrix control. It lifts Qooxdoo JS, MathJax, Postgres and more. The average number of dependencies for one value is a little more than one, and the deepest dependency chain is about a dozen. On complex dispays of many math problems, a little over a thousand values are dependent on other values.

This is the story of another 80KLOC Matrix app, a [clinical drug trial management system](http://smuglispweeny.blogspot.com/2008/03/my-biggest-lisp-project.html) with dataflow extended even deeper into a persistent Lisp object system (CLOS) database.

### Summary
We began with a mystery: how does rewiring reads and writes yield a new approach to application development? 

Part of the mystery was solved when we learned that A could be defined declaratively as a function of B.

We then saw that the rewiring automatically captures the fine-grained dependency graph (DAG) implicit in A reading B. With the DAG in hand, we can update state (including the DOM) reliably and efficiently.

Applications are built up property by property in small, declarative, functonal formulas. Being small, they are easy to write, debug, and revise. They are functional yet fast, caching computations. Cache invalidation is automatic and precise thanks again to the captured DAG.

We saw all the above applied to the model as well as to the view, and then to external libraries. The coding "wins" are enjoyed across the whole application.

And that is how, simply by changing what it means to read and write properties, mxWeb&trade; makes web pages easier to build, debug, and revise.   

### Postscript: on mutation
Clojurians understand well the danger of mutation. Via the `re-frame` doc we have:
<div style="width:400px">
  <blockquote class="twitter-tweet" lang="en"><p>Well-formed Data at rest is as close to perfection in programming as it gets. All the crap that had to happen to put it there however...</p>&mdash; Fogus (@fogus) <a href="https://twitter.com/fogus/status/454582953067438080">April 11, 2014</a></blockquote>
</div>
That said...

> "Nothing messes with functional purity quite like the need for side effects. On the other hand, effects are marvelous because they move the app forward." - [re-frame intro](https://github.com/Day8/re-frame)

<img height="350px" align="right" src="/image/tododag400.png?raw=true">

One-way, dependency graphs are examples of *directed acyclic graphs* or *DAGs*. To the right we see a diagram of perhaps half of the TodoMVC DAG. And TodoMVC is a trivial dataflow problem, with few derived states computing from unrealistically few input states. Real-world applications have real-world DAGs which defy manual propagation.  

Matrix, re-frame, MobX (JS) and other glitch-free reactive libraries make state change coherent and reliable:
* derived state is functionally declared;
* state flows "one-way";
* by recording reads property by property, a detailed dependency graph emerges so...
* ...when mutations move the app forward, efficiency and consistency are guaranteed. 
#### Data integrity
From the Common Lisp [Cells Manifesto](http://smuglispweeny.blogspot.com/2008/02/cells-manifesto.html), our definition of so-called *data integrity*:
<blockquote>
When application code assigns a new value to some input cell X, the Cells engine guarantees:
<ul>
    <li>recomputation exactly once of all and only state affected by the change to X, directly or indirectly through some intermediate datapoint. Note that if A depends on B, and B depends on X, when B gets recalculated it may come up with the same value as before. In this case A is not considered to have been affected by the change to X and will not be recomputed;</li>
    <li>recomputations, when they read other datapoints, must see only values current with the new value of X. Example: if A depends directly on B *and* X, and B itself depends on X, then when X changes and A reads B and X to compute a new value, B must return a value recomputed from the new value of X;
    </li>
    <li> similarly, client observer callbacks must see only values current with the new value of X; and...</li>
    <li>...a corollary: should a client observer write to a datapoint Y, all the above must happen with values current with not just X, but also with the value of Y *prior* to the change to Y.</li>
    <li> deferred "client" code must see only values current with X and not any values current with some subsequent change to Y queued by an observer.</li>
</blockquote>

## Building TodoMVC from Scratch
That completes our high level look at Matrix, mxWeb, and a bit of TodoMVC. 

To see the full mxWeb treatment of TodoMVC, switch to the branch "building" in this repo.

A much deeper explication of mxWeb can be found in our annotated, [stepwise evolution of TodoMVC](documentation/BuildingTodoMVC.md).

