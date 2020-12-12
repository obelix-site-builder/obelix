---
---
# Obelix: simple & reliable static site generator

![obelix the gaul](images/obelix.jpg)

Obelix is a [static site generator](https://www.netlify.com/blog/2020/04/14/what-is-a-static-site-generator-and-3-ways-to-find-the-best-one/). Its primary goals are simplicity, ease of use, and extensibility. Out of the box, Obelix supports:

- [CommonMark](https://commonmark.org/)-compliant markdown rendering
- Page and post metadata via YAML frontmatter
- String templating powered by [Handlebars](https://handlebarsjs.com/)
- Layout templates to apply a common layout to the whole site or a subdirectory
- List templates to render index pages, feeds, or any other listing of a subdirectory

In addition, Obelix features a powerful plugin system that allow developers to extend sites however they want using JavaScript.

## Installation
Obelix is available [on NPM](npmjs.com/obelix). It's meant to be installed globally:

```
$ sudo npm install -g obelix
```

## Configuration
All Obelix sites have an `obelix.json` configuration file at the top level. At a minimum, this file needs to contain two keys, `"src"` and `"out"`:

```json
{
    "src": "source",
    "out": "build"
}
```

The `"src"` key should be the relative path to the directory where your site's source files live. The `"out"` key is the relative path to the directory where Obelix will output the built site.
