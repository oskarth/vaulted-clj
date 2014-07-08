# Installation

[![Clojars Project](http://clojars.org/vaulted/vaulted-clj/latest-version.svg)](http://clojars.org/vaulted/vaulted-clj)

# Usage

Either set the environment variable `VAULTED_API_KEY`, or wrap each
function call in a `wrap-token` call: `(with-token "my-api-key" ...)`.

The public functions can be found in `core.clj`. You can also use the
primitive functions `get/post/put-resource` if you want to do
something with a resource that isn't supported yet.

Data validation is possible by using `schema` and the Schemas defined
in `core.clj`. Since keys are subject to change, this validation is
optional. Informative error message should appear as exceptions if you
don't give the API the right data.

Further documentation can be found at:
[https://vaulted.com/merchant/api](https://vaulted.com/merchant/api).

Bug reports and feature requests to @oskarth.
