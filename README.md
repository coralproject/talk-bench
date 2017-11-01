# talk-bench
A script to benchmark Talk.

# Build

You can build the benchmarking script using the following command:
```bash
./gradlew clean jarsh
```

This will create a `talk-bench.sh` script in the `build/libs` directory. The script will have all of its dependencies embedded so you can port it to any other machine.

# Usage
```bash
./talk-bench.sh --help
Usage: <main class> [options]
  Options:
    -i, --asset-id
      The asset id of the story to which the comments are posted
    -c, --count
      The number of comments to post during the benchmark
      Default: 0
    -e, --email
      The user email
    -h, --help
      Show the help text
      Default: false
    -p, --password
      The user password
    -u, --root-url
      The Talk Root URL
    --verbose
      Log extra information for debugging
      Default: false
```

