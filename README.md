# Figura

The Figura Project is dedicated to re-imagining the way avatars in minecraft work, as a way to allow players to more creatively express themselves.

The goal is to achieve this in a purely client-side mod, using an external server to store and serve avatars, and to create a mod that enables users to create custom, interesting avatars that do not risk the safety or performance of other users.

Figura is specifically build to allow creators as much creative freedom as possible. This comes in the form of an extremely easy to use creation pipeline, and a verbose, extensive lua API for reading data from Minecraft.

# Creation

Model Creation is primarily achived through five steps:
1. Bedrock Models created in blockbench, and then imported into the game while using the mod. Special names are used to assign model parts to specific vanilla body parts for animation syncing, as well as rending some parts with/without certain armor or held items, during certain animations, etc. This enables a lot of customization with **little to no scripting**.
2. Texture files created as one would normally create a .png file. No limit on size for the image itself, though it is included under the maximum 100kb file size limit for any avatar.
3. [OPTIONAL] .Obj files created through Blender or similar programs. These allow for non-cube models to be added to your avatar, for more creativity. Note that importing these is typically significantly more expensive than using Blockbench parts.
4. [OPTIONAL] Creating a .lua script file to indicate any custom behaviour your avatar may have.
5. Import all above asset files with a single click, and upload the entire avatar with another click.


# Features
Notable features for the mod include

**Figura is Client-Side Only**
  - Figura is indented to be a fully client-side mod. User Avatars are retrieved from and uploaded to a server run by the mod's creator, Zandra. This means a Minecraft server does not need the mod installed for users to see your avatar.
  - Other users **are** required to have the mod installed to see your avatar, and you must have it installed to see theirs. It's physically impossible to change this, so DO NOT ask for it.

Client-Side Performance limitations.
  - Each user can decide on the performance settings for everyone they see individually. This means that someone with a lower-end PC can set avatars to render at less detail, while someone with a beast PC can set things to have practically no detail limit.
  - Model Complexity is limited on the fly. Model parts are rendered according to the hierarchy, allowing creators to prioritize specific details over others. Quads/Tris on mesh models are limited using the same system.
  - Lua Script Execution is limited. Scripts run by non-trusted users are limited to a specific instruction count, ensuring that it is impossible for random users to destroy framerate using just "while true do end". Similarly, Lua Scripts are incredibly sandboxed, not allowing for any systems beyond basic math libraries and executing very safely guarded Java code. This ensures users cannot write malicious scripts. On top of both of these factors, lua scripting is multi-threaded, ensuring that the main game's FPS will never drop due to a script executing slowly (though framerate can still drop due to rendering)


Easy Creation
  - Models are simply blockbench projects. They work 1:1 with Blockbench, enabling easy comparison and creation. Animations/Particles are not yet supported.
  - Texture mapping with Models/OBJ Models match exactly as you'd expect. There's no difficult setup. If your texture maps correctly in Blockbench or whatever software you use for OBJ files, it will match in-game.
  - **All files hotswap once initially loaded, enabling extremely fast iteration time.** In layman's terms: Once you load an avatar from local files, any changes to those local files will show in-game immedaitely, without even re-starting the game. However, other people will NOT see your new until you press the upload button. Once you've pressed it, it takes 10~ seconds for the model to update for them.

Creative Freedom
  - The goal of Figura is to allow creators to create freely without worry of limitations like rendering or scripts. The file size limit is the only real limit on creations, everything else is determined by a user's personal settings, which can always be turned off, or on. This allows for full creative freedom for creators, and safe viewing for everyone else..


# Plans
Plans for the future of Figura (sorted more or less by priority) include

1. Karma System
  - A system for managing player behaviour. Getting reported hurts your karma, while not getting reported rewards you karma. Users will be able to limit avatars based on someone's karma, allowing for a sort of "crowd moderation" system.
2. Functional Shaders
  - Allowing users to write their own shaders for avatars/parts of avatars. This will be done in 1.17 with the rendering system updates.
3. Network API
  - Allow the scripting API the ability to send small amounts of information to anyone viewing someone's avatar using a messaging system. Severely rate & data limited, but it will be routed through the Figura server, keeping Figura's "client-only" design in place.
4. Player Worlds
  - Allow players to upload a structure file generated using structure blocks to the Figura server, and allow other players to "visit" those structures in an empty world. These worlds could be used for showing off avatars, or just builds, or anything else.
5. Part Browser/Avatar Stitching
  - The final planned feature for moving Figura out of alpha/beta will be the ability for players to use an in-game browser to pick "parts" from publicly avaliable sets, and automatically use those parts together into a single avatar.
