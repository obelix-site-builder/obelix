---
---
# Obelix: simple & reliable static site generator

![obelix the gaul](images/obelix.jpg)

Obelix is a [static site generator](https://www.netlify.com/blog/2020/04/14/what-is-a-static-site-generator-and-3-ways-to-find-the-best-one/). Its primary goals are simplicity, ease of use, and extensibility.

In a nutshell, a static site generator transforms a set of input data into static assets (HTML, CSS, JavaScript, images, etc.) ready to be served by a web server. The input data can come from a wide variety of sources, from local files on disk to APIs.

Out of the box, Obelix supports:

- [CommonMark](https://commonmark.org/)-compliant markdown rendering
- Page and post metadata via [YAML](https://rollout.io/blog/yaml-tutorial-everything-you-need-get-started/) frontmatter
- String templating powered by [Handlebars](https://handlebarsjs.com/)
- Layout templates to apply a common layout to the whole site or a subdirectory
- List templates to render index pages, feeds, or any other listing of a subdirectory
- A powerful plugin system that allows developers to write JavaScript to pull in data from external APIs, transform existing data before it gets rendered, or anything else you can imagine

## Installation
Obelix is available [on NPM](npmjs.com/obelix). It's meant to be installed globally:

```
$ sudo npm install -g obelix
```

## Getting started
An Obelix site consists of a directory containing the source files for the website and an `obelix.json` configuration file. At a minimum, this file needs to contain two keys, `"src"` and `"out"`:

```json
{
    "src": "source",
    "out": "build"
}
```

The `"src"` key should be the relative path to the directory where your site's source files live. The `"out"` key is the relative path to the directory where Obelix will output the built site. For example, the directory for the site described by the `obelix.json` above might look like this:

```
.
├── obelix.json
└── source
    ├── index.md
    └── # all site source files here
```

To build the site, run:

```
$ obelix build
```

This will parse through all file in the source directory, transform them as necessary, and render the final site to the output directory (creating it if necessary). Any markdown files will get transformed to HTML, frontmatter and Handlebars template expressions will be processed, and layout and list templates will be applied.

There are a few other keys you can put in `obelix.json`:

- `"metadata"`: this should be a JSON object containing site metadata. This object will exposed in Handlebars templates as the `site` key (see below)
- `"plugins"`: An object mapping plugin names to configuration options. More details on this in the Plugins section below

## Source file configuration
All source files in an Obelix site are considered either an `asset` or a `page`. A `page` is a text file (with any file extension) where the beginning of the file contains [YAML](https://rollout.io/blog/yaml-tutorial-everything-you-need-get-started/) frontmatter. YAML frontmatter is used to specify metadata about the page and consists of the characters `---`, followed by a newline and a YAML document containing data you want, and finally another newline and the closing characters `---`. For example:

```
---
author: Getafix
title: Little-known herbs of Gaul
published: 2020-12-17
tags:
  - blog
  - druidism
---
And then the page content goes here!
```

Obelix performs some default transformations on `page`-type sources, including expanding Handlebars template expressions and applying layout templates, and exposes these files as data in list templates (more on all this later).

Any file that does not contain YAML frontmatter, including image or other non-text files, are considered `asset`-type sources. Obelix does no additional processing on `asset` files - it just copies them verbatim to the output directory. 

**Important**: Even if you have no metadata you want to attach to a page, you need to put YAML frontmatter on it for Obelix to process it. In these cases, you can just put the opening `---` and closing `---` with no content in between.

The source directory structure determines the output directory structure. For example, a source file `blob/post1.md` would get written to the output directory as `blog/post1.html`.

## Template expansion
`Page`-type source files can contain [Handlebars](https://handlebarsjs.com) template expressions. Template expressions are delimited by double curly braces - for example, `\{{ title }}`. Any page metadata in the page's frontmatter is available as a variable for use in a template expression, and any site metadata set in `obelix.json` is available in the `site` object in template expressions. 

For example, given the following `obelix.json`:

```json
{
    "src": "source",
    "out": "build",
    "metadata": {
        "publisher": "When In Rome LLC"
    }
}
```

And the following `page` source `post.md`:

```
---
author: Caesar
---
This post was written by \{{ author }} and published by \{{ site.publisher }}
```

The output file `post.html` would render like this:

```html
<html>
  <body>
    <p>This post was written by Caesar and published by When In Rome LLC</p>
  </body>
</html>
```

Handlebars offers many additional templating features - see [the official guide](https://handlebarsjs.com/guide/) to learn more.

## Layout templates
Layout templates allow you to apply a unified layout to `page` sources. By default, layout templates are any file named `layout.html.hbs` or `layout.html.handlebars`, but these defaults can be overridden by the `"layoutTemplates"` `obelix.json` field, which should be an array of layout template names. Individual pages can also specify a `template` metadata field, which should be the name of the file to use as a layout template for that page. Layout templates apply to all `page` sources in the same directory they are in and in subdirectories, but if a subdirectory has its own layout template that template overrides the parent layout template. This means you can put a `layout.html.hbs` at the root of your site that will be applied by default to every `page` in the site, but you can override that template for individual subdirectories by giving them their own `layout.html.hbs`.

A layout template is a Handlebars template that gets passed the content of the page it is applied to as the `content` key. When rendering a `page`, if Obelix finds a layout template for that page it will render the layout template and replace the output content with the result. 

An example should make things clearer. Given this `page` source `post.md`:

```
---
---
# My Post
This is some hot content!
```

And this `layout.html.hbs`:

```html
<html>
  <head>
     <link rel="stylesheet" href="styles.css" />
  </head>
  <body>
    <div class="container">
       \{{{ content }}}
    </div>
  </body>
</html>
```

The output file `post.html` would look like this:

```html
<html>
  <head>
     <link rel="stylesheet" href="styles.css" />
  </head>
  <body>
    <div class="container">
      <h1>My Post</h1>
      <p>This is some hot content!</p>
    </div>
  </body>
</html>
```

Note the use of triple curly braces in `\{{{ content }}}`. This tells Handlebars not to HTML-escape the result of the expression. See [here](https://handlebarsjs.com/guide/#html-escaping) for more info.

## List templates
A list template is like a layout template, but instead of being passed a single page it gets passed a list of pages. List templates can be used to generate index pages, RSS feeds, or any other collection of content. A list template is simply any file with a `.hbs` or `.handlebars` file extension that isn't a layout template. Unless you have the `"layoutTemplates"` configuration option set, this means that any `.hbs` or `.handlebars` file that isn't named `layout.html.hbs`, `layout.html.handlebars`, or is the target of a `template` page metadata will be treated as a list template.

List templates get passed an array of all pages in the same directory as them as the `pages` variable. Each item in this list is a `page` object that the one that gets passed to layout templates - it will have a `content` key containing the page content in addition to any keys in the page frontmatter. The `pages` array can be iterated over using the [Handlebars `each` helper](https://handlebarsjs.com/guide/builtin-helpers.html#each):

```html
<html>
    <body>
      \{{#each pages}}
          <h1>\{{ title }}</h1>
          {{{ content }}}
      \{{/each}}
    </body>
</html>
```

If you want to iterate over pages that aren't in the same directory as the list template, you can use the `site.pages` variable. This variable is a nested array of all pages in the site. The format of `site.pages` is a little weird: it's a recursive array that can be iterated over to access the pages in the top-level directories, but exposes subdirectories as attributes on that array with values that are themselves recursive arrays. For example, given the following site structure:

```
.
├── index.md
├── about.md
└── blog
    ├── post1.md
    └── post2.md
```

The `site.pages` variable would be an array containing `index.md` and `about.md`, and `site.pages.blog` would be an array containing `post1.md` and `post2.md`.

If you have a directory whose name isn't a valid JavaScript identifier, you can access it using index notation, e.g. `site.pages["My weird folder"]`. Although this layout is a bit unconventional, it makes it convenient to loop through pages using the `\{{each}}` helper at any level in the directory structure.

List templates can be either `page` or `asset` sources. If you include a frontmatter block in a list template, it will be treated as a `page` and have layout templates applied to it. If not, it will be treated as an `asset` and layout templates will not be applied to it.

## Custom Handlebars helpers
Obelix comes with a couple of [custom Handlebars helpers](https://handlebarsjs.com/guide/#custom-helpers) and exposes the ability to define your own custom helpers specific to your site.

The in-the-box custom helpers are `sort` and `reverse`. Both operate on arrays. `Sort` sorts its input array. By default, `sort` will use whatever built-in comparison operation JavaScript defines for the contents of the array, but for the special case of an array of objects `sort` can be passed the `key` [hash argument](https://handlebarsjs.com/guide/expressions.html#helpers-with-hash-arguments), in which case it will sort by comparing the value of the specified `key` in each object in the array.

The `reverse` helper does what it says on the tin - it reverses the input array.

You can register your own custom Handlebars helpers by passing a `"handlebarsHelpers"` option in `obelix.json`. This should be the path to a JavaScript module which should return a single object. The returned object's keys are the names of the custom helpers and the values should be the functions defining each helper. See [the Handlebars documentation](https://handlebarsjs.com/guide/expressions.html#helpers) for details on writing custom helpers.

## Plugins
Still writing this part. Check back soon!
