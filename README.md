# crates-caching-proxy

A Clojure-based caching proxy for Cargo crates downloaded from
[crates.io](https://crates.io). The primary intent of the application
is to have a functioning build while the crates.io site is down. Note
that there are two pieces to downloading crates, the first is the
[create-io.index](https://github.com/rust-lang/crates.io-index) and
the second is the packages themselves. This project is only concerned
with storing the crates themselves, not the index.

## Usage

The entire application is a single clojure file, launched via `lein
crate-proxy`. By default the proxy will launch on port 8888 and cache
the crates in the `proxy-cache` directory, wherever it was
launched. To override the port and the cache directory, use a command
like: `CACHE_ROOT="/full/path/to/dir" CRATE_PROXY_PORT=9090 lein crate-proxy`.

## Maintenance

Maintainers: Ryan Senior <ryan.senior@puppet.com>
