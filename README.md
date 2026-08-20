# Global Resource Flow Intelligence Platform

Global resource flow visualization platform. Aggregates worldwide resource statistics, infers time-series flows, and renders interactive visualizations.

## Live

- https://global.etzhayyim.com

## MCP Endpoint

```
POST https://actors.etzhayyim.com/{nanoid}/api/mcp
Content-Type: application/json

{"jsonrpc":"2.0","method":"tools/list","id":1}
```

### Tools

| Tool | Description |
|---|---|
| `global.list_resources` | List global resources |
| `global.list_flows` | List resource flows (filter by resource_id, year) |
| `global.get_graph` | Build resource graph for 3D visualization |
| `global.get_resource_stats` | Get region stats for a resource |
| `global.get_timeline` | Get timeline data for a resource |
| `global.list_systems` | List system models |
| `global.get_system` | Get a full systems-thinking model |

## Project Structure

```
PROJECT.jsonld              # Project metadata (JSON-LD)
scheduler.jsonld            # Milestone tracking
MCP_TOOLS.md                # MCP tool contract documentation
README.edn                  # Machine-readable self-description
migration.edn               # Provenance: what was extracted, and from where
facts/catalog.edn           # Cited public sources, with a declared floor
tools/verify_citations.cljs # Live gate: every citation must return 2xx
test/run.cljs               # Offline gate: the records parse and agree
```

Every path above exists — `test/public_global/records_test.cljc` reads this
block and fails if one does not. It was added because this list previously
described a `wasm/global-mcp-component/` tree that the extraction never
carried (`migration.edn` records `:go-files-created 0` in the same directory),
so a reader followed the README to files that had never been here.

**The wasmCloud MCP component is not in this repository.** The tool contract it
implements is `MCP_TOOLS.md`; the component itself stayed behind in
`etzhayyim/root` at the revision `migration.edn` names.

## Gates

```bash
nbb --classpath "test:." test/run.cljs        # offline: parses, shapes, this README
nbb tools/verify_citations.cljs               # live: every cited URL returns 2xx
```

The two are separate because they answer different questions and fail for
different reasons. The live gate is worth what the network is worth at the
moment it runs; the offline one settles what the bytes here can settle.

## License

Apache License 2.0 — see [LICENSE](LICENSE).
