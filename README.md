# Teaforall Missiles
An easy solution for these elytra-pests that infects your world.

'Teaforall Missiles' is server-side fabric mod for Minecraft that modifies the behaviour of crossbow-shot firework rockets into manual-guided and self-guided missiles that seeks out your enemies.

## Changed Vanilla Features
Fireworks now move at `2` blocks per tick when shot from a crossbow.

When fireworks are crossbow fired when unguided, they will experience and accumulate slight inaccuracy.

## Guided Missiles
### How to obtain
1. Make a firework star with either `Trail` or `Twinkle`
2. Place the crafted firework star in the top left corner of the crafting menu
3. Fill in the rest and craft a firework rocket
4. This firework rocket will be special and will be guided

*****Make sure the firework star is the top left so that the firework star shows up as the first firework component in tooltips. It will not work if otherwise.**

These fireworks now have guidance depending on which 'effect' is present in the first component.

They will last 4x longer and will not be affected by the inaccuracy that unguided rockets now do.

### Manual Guidance
When equiped with the `Twinkle` effect, these fireworks fired from any crossbow can now be manually guided based on where your players is looking at.

They travel at a constant speed of `2` blocks per tick, and will explode and deal the same damage as ordinary fireworks.

The max turning rate of these fireworks is roughly `100` degrees per second. (will probably make this configurable... someday)

### Automatic Guidance
"Tracking Mode" will occur when a player holds a crossbow loaded with a `Trail` effect firework.

During this mode, this player can looking at any LivingEntity for `1` second to track it. 

If the player looks away or looks at a closer entity to their crosshair, then this lock is broken.

When a target successfully tracked, the firework will automatically seekout target. The guidance accounts for the target's velocity and will lead to close the distance. 

If shot without a lock, the firework will become unguided.  

