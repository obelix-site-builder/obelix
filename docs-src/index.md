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

You can also run:
```
$ obelix serve
```

This will start a web server serving your site on port 8080 (by default - pass the `-p` option to change this). The server will automatically rebuild the site whenever it detects changes to a source file. This is a just a development convenience - the `obelix serve` server is not production-ready!

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

Tip: Obelix adds a `url` metadata field to every page by default. This is especially useful in list templates as it lets you construct a link to the pages that the list template is rendering.

## Data files
Source files with a `.json`, `.yaml`, or `.yml` extension are considered data files. Data files are a way to store structured data that isn't meant to be displayed literally. Obelix parses all the data files it finds and passes them into list and layout templates as the `site.data` variable. This variable is a dictionary of data file paths to data file contents. If the data file is in a subdirectory, the path to the data in `site.data` matches the path to the data file. For example, the data file `foo/bar/baz.json` would be exposed in templates as `site.data.foo.bar.baz`.

Data files can have YAML frontmatter. If they do, they are passed through the same Handlebars templating step as `page`-type sources and can contain Handlebars expressions referencing the frontmatter.

By default, data files are not rendered to the output directory. If you want a literal `.json` or `.yaml` file in the output directory, give the data file the frontmatter expression `render: true`.

## Custom Handlebars helpers
Obelix comes with a couple of [custom Handlebars helpers](https://handlebarsjs.com/guide/#custom-helpers) and exposes the ability to define your own custom helpers specific to your site.

The in-the-box custom helpers are `sort` and `reverse`. Both operate on arrays. `Sort` sorts its input array. By default, `sort` will use whatever built-in comparison operation JavaScript defines for the contents of the array, but for the special case of an array of objects `sort` can be passed the `key` [hash argument](https://handlebarsjs.com/guide/expressions.html#helpers-with-hash-arguments), in which case it will sort by comparing the value of the specified `key` in each object in the array.

The `reverse` helper does what it says on the tin - it reverses the input array.

You can register your own custom Handlebars helpers by passing a `"handlebarsHelpers"` option in `obelix.json`. This should be the path to a JavaScript module which should return a single object. The returned object's keys are the names of the custom helpers and the values should be the functions defining each helper. See [the Handlebars documentation](https://handlebarsjs.com/guide/expressions.html#helpers) for details on writing custom helpers.

## Using plugins
Obelix plugins allow you to extend the site building process in any way you want, from adding new page and data sources to transforming content to writing output to third-party systems. You can install and distribute third-party plugins via `npm`, and you can also write plugins local to your site that live alongside the site source.

Using a plugin in your site has two steps: first, you have to install the plugin, then you have to configure the plugin in your `obelix.json`. Third-party plugins can be installed by `npm install`-ing them into your site directory. Local plugins should be placed in a directory called `plugins` at the same level as `obelix.json` (for more info on writing your own plugins, see below).

Once the plugin is installed, it must be configured. To do this add a field to `obelxix.json` called `"plugins"`. This should be a map where the keys are plugin names and the values are plugin configuration objects. Here's an example `plugins` section:

```json
{
  "src": "src",
  "out": "out",
  "plugins": {
    "obelix-plugin-typography": {
      "fileName": "css/typography.css",
      "theme": "typography-theme-us-web-design-standards"
    },
    "my-local-plugin": {}
  }
}
```

The contents of the configuration section is specific to each plugin. Note that even plugins which require no configuration, like `"my-local-plugin"` above, still need to have an empty configuration object in `obelix.json` to be registered.

## Writing plugins

*Note: this is an advanced topic. You don't need to read it unless you are interested in extending Obelix in some way beyond its default capabilites*

To write an Obelix plugin, there are a couple of concepts to understand: the Obelix data model and the steps of the build pipeline.

### The Obelix data model
Internally, Obelix uses a simple JSON structure to represent a site. Its general shape is as follows:

```javascript
{
  "metadata": {
    // any metadata general to the site as a whole
  },
  "routes": [
    {
      "name": "relative/path/to/file.md",
      "type": "page", // or "asset" or "data"
      "content": "Page content here",
      "metadata": {
        // page-specific metadata including frontmatter here
      }
    }
  ]
}
```

The `routes` field contains all pages, assets, and data files for the site. Note that `routes` is a flat array - any content hierarchies are implied by directory separators within the `name` field of each element.

### The build pipeline
The Obelix build process is implemented as a pipeline of functions. Each function takes as an argument the data structure defined above, and returns that data structure modified in some way. The output from the previous step becomes the input to the next step in the pipeline.

The pipeline consists of the following steps:

  1. **`source`**: In this step, Obelix traverses the source directory and inserts entries into the `routes` array for all files it finds. Files are marked as `"type": "asset"` by default, but if Obelix finds a frontmatter block at the beginning of a file it reads the frontmatter in the `"metadata"` field of the `routes` array entry and marks the entry as `"type": "page"` instead. In either case, the file's contents is read as a raw [`Buffer`](https://nodejs.org/docs/latest-v15.x/api/buffer.html) object into the `"content"` field.

  2. **`markdown`**: In this step, Obelix processes any entries in the `routes` array with a `.md` or `.markdown` extension. It reads the `"content"` field of the entry and parses it from Markdown into HTML, writing the result back into the `"content"` field. It also changes the file extension of the entry to `.html`.
  
  3. **`url`**: In this step, Obelix adds a `"url"` field to the `"metadata"` of each node in the `routes` array. This is done simply by taking the `"name"` of the entry and adding a leading `/`, thus creating an absolute URL for that page that is available for use in templating.

  4. **`template`**: In this step, Obelix resolves any Handlebars expressions within page sources. For every entry in the `routes` array with `"type": "page"`, Obelix compiles the `"content"` field as a Handlebars template and executes that template, passing the page `"metadata"` map as the Handlebars template data. Obelix also adds any site metadata (the top-level `"metadata"` field in the data model) as the `"site"` variable within the Handlebars template. The result of the templating is written back to the entry's `"content"` field.

  5. **`data`**: In this step, Obelix parses data files. It looks through all entries in the `routes` array with extension `.json`, `.yml`, or `.yaml`, and if finds any parses their `"contents"` field as a data file of the appropriate type (using a JSON or a YAML parser). The resulting data structure is written to the entry as the `"data"` field (note that this step does not change the entry `"contents"`). This step also changes the `"type"` of the entry to `"data"`.
  
  6. **`listTemplate`**: In this step, Obelix renders all list templates that it finds. First, it identifies `routes` array entries that are list templates by selecting all entries with the file extension `.hbs` or `.handlebars` that are not layout templates (see the next step for how layout templates are identified). For any of these entries that it finds, it constructs a Handlebars context for the list template and renders the template back to the entry's `"content"` field. 
  
  The Handlebars context is constructed as follows: Obelix finds all `"type": "page"` entries whose `"name"` indicates that they are in the same directory as the list template and for each one, constructs an object whose keys are the page's metadata entries plus the special key `"content"` containing the page's content; it also constructs the `site` object, which consists of the `"pages"` and `"data"` fields mentioned in the list templates section above plus any defined site metadata. The `site.data` field uses the `"data"` key added to entries in the `data` step. The `pages` and `site` objects, as well as any `"metadata"` defined on the list template, get passed as fields in the Handlebars context to the list template. 
  
  Note that this step does not change the `"type"` of the list template, so if the list template was `"type": "page"` it will remain that way and have layout templates applied to it in the `layout` step.
  
  7. **`layout`**: In this step, Obelix applies layout templates to `"type": "page"` entries in the `routes` array. For each `"type": page` entry, it first identifies which layout template (if any) should apply to that page. To do this, it first checks for a `"template"` key in the entry `"metadata"`. If there isn't one, it looks for entries in the same directory or higher called `layout.html.hbs` or `layout.html.handlebars` (unless there is a `layoutTemplates` field in `obelix.json`, in which case it looks for files with those names). 
  
  If it finds a layout template by either of those methods, Obelix applies the layout template. It constructs a Handlebars context that consists of any page `"metadata"`, a `"content"` variables holding the page content, and the same `"site"` object as described in the `listTemplate` step; it then passes this context object to the Handlebars template in the layout template's `"content"` field and replaces the `"content"` field of the target page with the rendered template. Finally, this step removes any layout templates from the `routes` array so that they don't get rendered to the output directory.
  
  8. **`html`**: In this step, Obelix formats any source files that it knows how to. Currently it only knows how to format `.html` files. It overwrites the page `"content"` field with the formatted version of the page HTML.
  
  9. **`output`**: In the step, Obelix writes the entries in the `routes` array to the directory defined as `out` in `obelix.json`. If the output directory already exists, Obelix deletes and recreates it, then it iterates through the `routes` array and writes the `content` of each entry to the file in the output directory that corresponds to the entry `"name"` field. Obelix automatically creates any needed sub-directories. Entries with `"type": "page"` or `"type": "asset"` are always written to disk, but entries with `"type": "data"` are only output if they have the field `"render": true` in their `"metadata"`.

### Plugin structure
After understanding the Obelix data structure and build pipeline, it's easy to grasp how a plugin is structured: a plugin is simply a collection of hooks that run at various stages during the build pipeline and manipulate the site data structure. A hook runs before every stage in the pipeline, and is named the same thing as the pipeline step. For example, the `"source"` hook runs before the `source` step and the `"listTemplate"` hook runs before the `listTemplate` step.

An Obelix plugin is a [Node.js module](https://nodejs.org/api/modules.html) that exports a single function. This function takes in the plugin configuration object as an argument and should return a plugin object, which is an object that contains a key called `"hooks"`. The `"hooks"` field of the plugin object is itself a map of hook names to hook function. A hook function is simply a function that takes in the site data map as its argument and returns a new site data map to be passed onto the next step in the build pipeline.

Here's an example of a really simple plugin that adds a metadata field to all pages in a site:

```javascript
function addGreeting(siteData) {
  siteData.routes.forEach(entry => {
    if (entry.type == "page") {
      entry.metadata.greeting = "Hello, world!";
    }
  });
  return siteData;
}

module.exports = config => {
  // config contains the plugin configuration, although this plugin
  // doesn't require any
  return {
    hooks: {
      template: addGreeting
    }
  };
};
```

This plugin only defines one hook, the `template` hook, but plugins can use as many hooks as they need to accomplish their goals.

If you want your plugin to be publicly available, consider publishing it on NPM; if you do, tag it with the tag `obelix-plugin` to help others discover it. If your plugin is just for your site, you can save it as a module in the `plugins` directory at the same level as your site's `obelix.json` (a Node.js module is either a single file, where the name of the file is the name of the module, or it is a directory containing an `index.js` file, in which case the name of the directory is the name of the module).

### Which hooks should you use?
Depending on the purpose of your plugin, there are a couple of different hooks you could consider using:

- If your plugin adds new data files or site assets, you should do this in the `source` hook
- If your plugin adds or mutates page metadata, you should do this in the `template`, `listTemplate`, or `layout` hooks, depending on how early in the build process users will need this metadata available
- If your plugin passes the Obelix site as input to some other service, such as an API, a CMS, a build system, a backup drive/service, or even just another output directory, you should use the `output` hook

Writing plugins can be complicated. If you run into any problems, [open a GitHub issue](https://github.com/obelix-site-builder/obelix/issues/new) and I'll try to get it sorted out.
