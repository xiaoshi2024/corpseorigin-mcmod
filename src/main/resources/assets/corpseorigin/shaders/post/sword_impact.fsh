#version 330

uniform sampler2D InSampler;
in vec2 texCoord;
layout(std140) uniform SamplerInfo { vec2 OutSize; vec2 InSize; };
layout(std140) uniform ImpactConfig { float Strength; };
out vec4 fragColor;

void main() {
    vec2 radial = texCoord - vec2(0.5);
    float edge = smoothstep(0.12, 0.65, length(radial));
    vec2 shift = radial * (0.007 * Strength * edge);
    vec3 base = texture(InSampler, texCoord).rgb;
    vec3 color = vec3(texture(InSampler, clamp(texCoord + shift, 0.0, 1.0)).r,
                      base.g, texture(InSampler, clamp(texCoord - shift, 0.0, 1.0)).b);
    vec3 blur = base;
    for (int i=1; i<=3; ++i) {
        blur += texture(InSampler, clamp(texCoord - radial * (float(i) * 0.004 * Strength), 0.0, 1.0)).rgb;
    }
    color = mix(color, blur * 0.25, edge * 0.35 * Strength);
    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(color, vec3(luma), 0.16 * Strength);
    color *= 1.0 - edge * 0.14 * Strength;
    fragColor = vec4(color, 1.0);
}
