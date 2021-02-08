# Figura

The Figura Project is a project dedicated to re-creating the VRChat Avatar System in Minecraft, as a way to allow users to more creatively express themselves.

The goal is to achieve this in a purely client-side mod, using an external server to store user asset files for in-game retrieval, that enables users to create custom, expressive avatars that do not risk the safety or performance of other users.



# Creation

This is primarily achived through five primary steps
1. Bedrock Models created in blockbench, exported from blockbench, and then imported into the game using mods. Special names are used to assign model parts to specific vanilla body parts for animation syncing and such.
2. Texture files created as one would normally create a .png file. No limit on size for the image itself.
3. .Obj files created through Blender or similar programs. These allow for non-cube models to be added to your avatar, for more creativity.
4. [OPTIONAL] Creating a .lua script file to indicate the behaviour of your avatar, animate it, that sort.
5. Import all above asset files with a single command, verify the avatar works correctly, export avatar to .nbt file, upload .nbt file to server


# Features
Notable features for the mod include

**Figura is Client-Side Only**
  - Figura is indented to be a fully client-side mod. User Avatars are retrieved and uploaded from/to a server run by the mod's creator, Zandra. This means a Minecraft server does not need the mod installed for users to see your avatar.
  - Other users **are** required to have the mod installed to see your avatar, and you must have it installed to see theirs. It's physically impossible to circumvent this issue.

--Client-Side Performance limitations.
  - Model Complexity is caluclated and limited on the fly, enabling users to limit how much performance is sunk into model detail. Parts are rendered linearly in the hierarchy, allowing creators to prioritize specific details over others. Faces on mesh models are limited using the same system.
  - Lua Script Execution is limited. Scripts run by non-trusted users are limited to a specific instruction count, ensuring that it is impossible for random users to destroy framerate using just "while true do end". Similarly, Lua Scripts are incredibly sandboxed, not allowing for any systems beyond basic math libraries and executing very safely guarded Java code. This ensures user cannot write malicious scripts.


Easy Creation
  - Models are simply blockbench models set to Bedrock mode. They work 1:1 with Blockbench, enabling easy comparison and creation.
  - Texture mapping with Models/OBJ Models match exactly as you'd expect. There's no difficult setup. If your texture maps correctly in Blockbench or whatever software you use for OBJ files, it will match in-game.
  - Loading Avatars in-game for preview is a single command, `/figura load_model [avatar_name]`. Saving avatars is just as simple, `/figura save_model [avatar_name]`, which results in an output .nbt file for the current avatar that you can upload to the servers.
  - **All files hotswap once initially loaded, enabling extremely fast iteration time.** In layman's terms: Once you load an avatar from local files, any changes to those local files will show in-game immedaitely, without even re-starting the game.

Creative Freedom
  - Creators are allowed to create nearly anything they like with the mod, and upload that as their avatar safely.
  - Creation is easy, and near-limitless in potentail, within the confines of Minecraft's own rendering system and the 100kb file size limit (It does't sound like much, but it goes a long way for raw model data). In the future, Figura will either use Canvas, or write it's own rendering system that's significantly more optimized (VBOS isntead of Immediate Rendering)


# Plans
Plans for the future of Figura include

1. Functional Shaders
  - Allowing users to write their own shaders for avatars/parts of avatars
2. Writing Blender Compatibility Scripts.
  - Support for exporting animation files, possibly blendshapes, and more(?).
