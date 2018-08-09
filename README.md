# TodoMVC, with Matrix Inside&trade;
*An introduction by example to Matrix dataflow and mxWeb*

mxWeb&trade; makes web pages easier to build, debug, and revise simply by changing what happens when we read and write properties:
* when B reads A, A remembers B; and
* when we write to A, A tells B.

Those are just the fundamentals. Let us understand them better through concrete examples.

#### B reads A
What does it mean for B to read A? It means B is expressed as an HLL function that reads A. 
````clojure
(li
    {:class (cF (when (<mget todo :completed)
                  "completed"))}
    ...)
````
The above is an excerpt from the TodoMVC implementation which we will evolve in the next introductory document. `li` makes a proxy LI instance and has the same API as the HTML; `cF` makes `:class` functional; and `<mget` is the Matrix property reader that remembers which property is asking.

In the next excerpt, the Matrix manages a model (the M in MVC) property. `cI` arranges for that property to tell functional reader properties when they have changed:
````clojure
(md/make ::todo-list
    :items-raw (cI nil)
    :items (cF (remove td-deleted (<mget me :items-raw)))
    :empty? (cF (empty? (<mget me :items))))
````
Those simple examples could reasonably be ordinary functions of the to-do list. Other properties are more expensive hence usefully cached, and even these trivial formulaics make the to-do list functional properties stand out from other code. Or they may require special tracking by so-called "observers" discussed below.

#### A tells B
What does it mean for A to tell B? When we imperatively change A, Matrix internals automatically and transparently recalculate B:
````cljs
(input {:class       "toggle"
        ::mxweb/type "checkbox"
        :onclick     #(mswap!> todo :completed
                         #(when-not [%] (util/now)})
````
`mswap!>` is a Matrix property writer that:
* changes the `:completed` property of the model todo; and
* *before returning* recomputes the :class property of the proxy `li` we saw above.

### Digging deeper
A few more fundamentals:
* on-change handlers, or "observers", may be supplied for A or B; and
* we might have a property K for "kids", such as the children of a parent DOM element.

#### on-change handlers: observers
CHanging properties manifest themselves as a useful application, if only to update a Web page, via on-change callbacks we call "observers". When `A` changes, ian observer can:
* mutate properties outside the Matrix graph; or
* enqueue Matrix writes to other properties for execution immediately after the current write.

In the example above, the `:class` property of a proxy `li` instance gained or lost the "completed" class as the user toggeled the model to-do's `:completed` property via an `onclick` handler. Great, but how does the actual DOM `li` classlist change?

The mxWeb library provides an observer for maintaining the DOM:
````clojure
(defmethod observe-by-type
  [:mxweb.base/tag]
  [property model new-value _ _]
  (when-let [dom (tag-dom model)]
      (case property
        ...others...
        :class (classlist/set dom new-value))))
````
mxWeb proxy instances know which DOM element they represent, and Matrix change tracking at the property level tells mxWeb precisely which DOM attribute needs updating. This obviates the need for VDOM generation and diffing.
Notes:
* we offer no example of a deferred write at this time. Those arise when applications have grown quite large.
* *caveat lectorum* we use "observer" in the strict dictionary sense: "monitor, not participant". Other libraries use it differently.

#### K for Kids
Formulas can compute more than mere descriptive properties such as "completed"; we might have `K` for "kids" holding the children of some parent, such as the LI nodes under a UL DOM list. In other words, the population itself of our application model can grow or shrink with events. We call a dynamic population of causally connected models a *matrix*.

> ma·trix ˈmātriks *noun* an environment in which something else takes form. *Origin:* Latin, female animal used for breeding, parent plant, from *matr-*, *mater*

We will not worry about all this code just yet, but here is how our TodoMVC will avoid rebuilding the full DOM list of to-dos when one is added or removed or the selection changes:
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
Simply by propagating change between functional properties, and manifesting those changes to the outside world, the Matrix library brings declarative, transparent, functional applications to life.

### Extending the scope: lifting
We explained above how the computed `:class` "completed" got propagated to the actual DOM classlist by an observer. That hints at the next fundamental, which we call "lifting". 

The DOM knows nothing about Matrix, so we developed sufficient "glue" code to make it seem as if it did. That is mxWeb: six hundred lines of code creating two classes (one for HTML tags, one for CSS Styles) and other code to translate HLL handlers into native handlers. In the full implementation of TodoMVC we will see even more systems lifted into the Matrix: routing, XHR, localStorage, and even the system clock.  

#### Really?
Can we really program this way? This 80KLOC [Algebra](https://tiltonsalgebra.com/#) Common Lisp app consists of about twelve hundred `A`s and `B`s, and extends into a Postgres database. Everything runs under matrix control. It lifts Qooxdoo JS, MathJax, Postgres and more. The average number of dependencies for one value is a little more than one, and the deepest dependency chain is about a dozen. On complex dispays of many math problems, a little over a thousand values are dependent on other values.

This is the story of another 80KLOC Matrix app, a [clinical drug trial management system](http://smuglispweeny.blogspot.com/2008/03/my-biggest-lisp-project.html) with dataflow even more deeply extended to a persistent Lisp object system (CLOS) database.

### Related work
> "Derived Values, Flowing" -- the [re-frame](https://github.com/Day8/re-frame/blob/master/README.md) tag-line

Matrix enjoys much good company in this field. We believe Matrix offers more simplicity, transparency, granularity, expressiveness, efficiency, and functional coverage, but in each dimension differs only in degree, not spirit. Other recommended CLJS libraries are [Reagent](https://reagent-project.github.io/), [Hoplon/Javelin](https://github.com/hoplon/javelin), and [re-frame](https://github.com/Day8/re-frame). Beyond CLJS, we admire [MobX](https://github.com/mobxjs/mobx/blob/master/README.md) (JS), [binding.Scala](https://github.com/ThoughtWorksInc/Binding.scala/blob/11.0.x/README.md), and Python [Trellis](https://pypi.org/project/Trellis/). Let us know about any we missed.


#### tl;dr summary
By rewiring the fundamental action of reading and writing properties, Matrix captures the dependency graph implicit in the application code we write. 

Because it is captured transaparently, we think only about our applications while coding. Because we build applications from small, declarative formulas, even the largest application decomposes naturally into manageable chunks. 

Because this formulaic authoring extends to model and not just view, we enjoy this automaticity more broadly. And because, with sufficent "glue" code, external libraries can be brought under the dataflow umbrella, entire applications can be animated. 

#### Postscript: on mutation
Clojurians understand well the danger of mutation. Via the `re-frame` doc we have:
<div style="width:400px">
  <blockquote class="twitter-tweet" lang="en"><p>Well-formed Data at rest is as close to perfection in programming as it gets. All the crap that had to happen to put it there however...</p>&mdash; Fogus (@fogus) <a href="https://twitter.com/fogus/status/454582953067438080">April 11, 2014</a></blockquote>
</div>
On the other hand...

> "Nothing messes with functional purity quite like the need for side effects. On the other hand, effects are marvelous because they move the app forward." - [re-frame intro](https://github.com/Day8/re-frame)

<img height="350px" align="right" src="/image/tododag400.png?raw=true">

One-way derived graphs are examples of *directed acyclic graphs* or *DAGs*. To the right we see a diagram of perhaps half of the TodoMVC DAG. And TodoMVC is a trivial dataflow problem, with few derived states and unrealistically few input states. Real-world applications have real-world DAGs that defy accurate hand implementation.  

Matrix, re-frame, MobX (JS) and other glitch-free reactive libraries make state change coherent and reliable:
* derived state is functionally declared;
* state flows "one-way";
* by recording reads property by property, a detailed dependency graph emerges so...
* ...when mutations move the app forward, efficiency and consistency are guaranteed. 

From the [Cells Manifesto](http://smuglispweeny.blogspot.com/2008/02/cells-manifesto.html):
<blockquote>
When application code assigns to some input cell X, the Cells engine guarantees:
<ul>
    <li>recomputation exactly once of all and only state affected by the change to X, directly or indirectly through some intermediate datapoint. Note that if A depends on B, and B depends on X, when B gets recalculated it may come up with the same value as before. In this case A is not considered to have been affected by the change to X and will not be recomputed;</li>
    <li>recomputations, when they read other datapoints, must see only values current with the new value of X. Example: if A depends on B and X, and B depends on X, when X changes and A reads B and X to compute a new value, B must return a value recomputed from the new value of X;
    </li>
    <li> similarly, client observer callbacks must see only values current with the new value of X; and...</li>
    <li>...a corollary: should a client observer write to a datapoint Y, all the above must happen with values current with not just X, but also with the value of Y *prior* to the change to Y.</li>
    <li> deferred "client" code must see only values current with X and not any values current with some subsequent change to Y queued by an observer.</li>
</blockquote>

## Building TodoMVC from Scratch
That completes our tl;dr distillation of Matrix, mxWeb, and a bit of TodoMVC. A much deeper explication can be found in our annotated, [stepwise evolution of TodoMVC](documentation/InDepth.md).

