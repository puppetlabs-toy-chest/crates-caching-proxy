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

To point cargo at your local proxy, first clone the
[create-io.index](https://github.com/rust-lang/crates.io-index). Change
the config.json in the root of the repository to indicate the local
proxy:

```
{
  "dl": "http://localhost:8888/api/v1/crates",
  "api": "http://localhost:8888/"
}
```

Commit that change to the local crates-io.index repository. In the
root directory of the cargo project you're wanting to build create a
`.cargo/config` file that points to the local create-io.index:

```
[registry]
index = "file:///local/path/to/creates.io-index"
```

When running `cargo build`, you will see `Updating registry...` which
will point to the new repostory location. The crates will be
downloaded to the `CACHE_ROOT` directory, subqequent cargo runs will
use the local cached copy.

It's important to note that cargo won't attempt to download crates
that already exist in the `~/.cargo/registry`. Make sure to clear that
directory so that cargo will go fetch the depdencies and ensure the
proxy is working.

## Maintenance

Maintainers: Ryan Senior <ryan.senior@puppet.com>
