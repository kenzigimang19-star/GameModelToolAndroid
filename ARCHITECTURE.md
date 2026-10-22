# Game Model Tool — universal architecture (Android)

## Layers
1. **UI**: file picker, preview, metadata, export actions.
2. **Core model**: format-neutral `ModelData` mesh, warnings, transform/camera.
3. **Format registry**: selects a parser plugin based on extension and file signature.
4. **Format modules**: isolated import/export code per family/game.
5. **Export pipeline**: validation and conversion from normalized mesh into a target format.

## Implemented first
- `ModelFormatPlugin` contract
- `FormatRegistry`
- OBJ import plugin with polygon triangulation and relative indices
- `ModelData` neutral geometry representation

## Planned modules
- General: GLB/GLTF, STL, FBX only if a suitable Android-compatible library is chosen
- RenderWare: DFF reader/writer, TXD reader/writer
- GTA V: YDR, YFT, YDD geometry modules; YTD texture dictionary module
- Other games: plugins added individually, only after format/version is identified

## Conversion rules
- A plugin must identify its exact format/version before parsing.
- Export to DFF is offered only if the model data can be represented by the target RenderWare variant.
- Textures export separately to TXD; geometry conversion alone does not preserve all original game metadata, shaders, skeletons, collision data, or fragment structures.
- Never label an extension as supported until real sample files have passed tests.
