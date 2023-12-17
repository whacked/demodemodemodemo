# generate a static binary using janet + musl

this example demonstrates shoehorning a janet+jpm build dependency and using it to build a statically linked hello world binary

enter the dev shell using `nix develop`, then `jpm build` to build the binary (defaults to build/static-sample)

to build directly, run `nix build` and execute `./result/bin/static-sample`

references:
- https://github.com/janet-lang/janet/discussions/1339#discussioncomment-7871217

