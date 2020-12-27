# Obelix# Obelix
> A simple & reliable static site generator

![obelix the gaul](docs-src/images/obelix.jpg)

Obelix is a [static site generator](https://www.netlify.com/blog/2020/04/14/what-is-a-static-site-generator-and-3-ways-to-find-the-best-one/). Its primary goals are simplicity, ease of use, and extensibility.

## Installation and usage
See the [usage documentation](https://obelix-site-builder.github.io/obelix) for installation and usage instructions.

## Development
Obelix is written in [ClojureScript](https://clojurescript.org/) and compiled to a [Node.js](https://nodejs.org/en/) script.

To compile the source you'll need GNU Make and Clojure installed. Run:
```
$ make out
```

This will build Obelix to the standalone script `main.js` in the `out` directory.

To run the tests, run:
```
$ clojure -A:test
```
