varying vec2 v_texCoord;
varying vec2 v_camPos;
varying vec4 v_color;
varying vec3 v_nearPoint;
varying vec3 v_farPoint;
varying vec3 v_positon;
varying vec3 v_eyePos;

uniform float u_gridMinPixelsBetweenCells;
uniform float u_gridCellSize;
uniform vec4 u_gridColorThin;
uniform vec4 u_gridColorThick;
uniform float u_gridSize;

uniform mat4 u_projTrans;
uniform mat4 u_viewTrans;

uniform float u_near;
uniform float u_far;

const vec3 planeNormal = vec3(0.0, 1.0, 0.0);


float log10(float x) {
    return log(x) / log(10.0);
}
float satf(float x) {
    return clamp(x, 0.0, 1.0);
}
vec2 satv(vec2 x) {
    return clamp(x, vec2(0.0), vec2(1.0));
}
float max2(vec2 v) {
    return max(v.x, v.y);
}
vec4 axis(vec3 fragPos3D, float scale, bool drawAxis) {
    vec2 coord = fragPos3D.xz * scale;
    vec2 derivative = fwidth(coord);
    vec2 grid = abs(fract(coord - 0.5) - 0.5) / derivative;
    float line = min(grid.x, grid.y);
    float minimumz = min(derivative.y, 1.0);
    float minimumx = min(derivative.x, 1.0);
    vec4 color = vec4(0.0, 0.0, 0.0, 1.0);

    //    discard fragments between lines


    // z axis
    if (fragPos3D.x > -0.15 * minimumx && fragPos3D.x < 0.1 * minimumx)
    color.z = 1.0;
    // x axis
    if (fragPos3D.z > -0.15 * minimumz && fragPos3D.z < 0.1 * minimumz)
    color.x = 1.0;
    return color;
}
vec4 gridColor(vec2 uv, vec2 camPos, vec3 fragPos3D, float t)
{
    vec2 dudv = vec2(
    length(vec2(dFdx(uv.x), dFdy(uv.x))),
    length(vec2(dFdx(uv.y), dFdy(uv.y)))
    );

    vec2 coord = uv;// use the scale variable to set the distance between the lines
    vec2 derivative = fwidth(coord);
    vec2 grid = abs(fract(coord - 0.5) - 0.5) / derivative;
    float line = min(grid.x, grid.y);
    float minimumz = min(derivative.y, 1.0);
    float minimumx = min(derivative.x, 1.0);

    float lodLevel = max(0.0, log10((length(dudv) * u_gridMinPixelsBetweenCells) / u_gridCellSize) + 1.0);
    float lodFade = fract(lodLevel);

    // cell sizes for lod0, lod1 and lod2
    float lod0 = u_gridCellSize * pow(10.0, floor(lodLevel));
    float lod1 = lod0 * 10.0;
    float lod2 = lod1 * 10.0;
    vec4 c = (axis(fragPos3D, 10.0, true) + axis(fragPos3D, 1.0, true))* float(t > 0.0);
    if (c.x==1.0 || c.z==1.0)
    {
        dudv *= 8.0;
    }
    // each anti-aliased line covers up to 4 pixels
    dudv *= 4.0;

    // Update grid coordinates for subsequent alpha calculations (centers each anti-aliased line)
    uv += dudv / 2.0;

    // calculate absolute distances to cell line centers for each lod and pick max X/Y to get coverage alpha value
    float lod0a = max2(vec2(1.0) - abs(satv(mod(uv, lod0) / dudv) * 2.0 - vec2(1.0)));
    float lod1a = max2(vec2(1.0) - abs(satv(mod(uv, lod1) / dudv) * 2.0 - vec2(1.0)));
    float lod2a = max2(vec2(1.0) - abs(satv(mod(uv, lod2) / dudv) * 2.0 - vec2(1.0)));

    uv -= camPos;

    if (c.x!=1.0 || c.z!=1.0)
    {
        c = lod2a > 0.0 ? u_gridColorThick : lod1a > 0.0 ? mix(u_gridColorThick, u_gridColorThin, lodFade) : u_gridColorThin;
    }
    //    if(fragPos3D.x > -0.1 * minimumx && fragPos3D.x < 0.1 * minimumx) {
    //        c = vec4(1.0, 0.0, 0.0, 1.0);
    //    }
    //    if(fragPos3D.z > -0.01 * minimumz && fragPos3D.z < 0.01 * minimumz) {
    //        c = vec4(0.0, 0.0, 1.0, 1.0);
    //    }

    // blend between falloff colors to handle LOD transition


    // calculate opacity falloff based on distance to grid extents
    float opacityFalloff = (1.0 - satf(length(uv) / u_gridSize));

    // blend between LOD level alphas and scale with opacity falloff
    c.a *= (lod2a > 0.0 ? lod2a : lod1a > 0.0 ? lod1a : (lod0a * (1.0-lodFade))) * opacityFalloff;

    return c;
}

float computeDepth(vec3 pos) {
    vec4 clip_space_pos = u_projTrans * u_viewTrans * vec4(pos.xyz, 1.0);
    return (clip_space_pos.z / clip_space_pos.w);
}

void main() {
    float t = -v_nearPoint.y / (v_farPoint.y - v_nearPoint.y);
    vec3 fragPos3D = v_nearPoint + t * (v_farPoint - v_nearPoint);
    float depth = computeDepth(fragPos3D);
    gl_FragDepth = depth;
    //    vec4 color = gridColor(v_texCoord, v_camPos);
    gl_FragColor = gridColor(v_texCoord, v_camPos, fragPos3D, t);
    //    gl_FragColor = vec4(fragPos3D,1.0);
    //    if(v_texCoord.x < 0.006 && v_texCoord.x > -0.006)
    //    {
    //        gl_FragColor.rgb=vec3(1.0,0.0,0.0);
    //    }
    //    if(v_texCoord.y < 0.004 && v_texCoord.y > -0.003)
    //    {
    //        gl_FragColor.rgb=vec3(0.0,0.0,1.0);
    //    }
    float axisThickness = 0.5;// Adjust for desired thickness
    float gridThickness = 0.1;// Adjust for grid line thickness

    vec3 viewDir = normalize(v_eyePos - fragPos3D);
    float viewAngle = dot(viewDir, planeNormal);

    // Adjust line thickness based on angle
    float angleFactor = smoothstep(0.0, 1.0, abs(viewAngle));
    float dynamicThickness = mix(axisThickness, axisThickness * 2.0, angleFactor);// Adjust multiplier as needed
    // Use dynamicThickness instead of axisThickness for closeToXAxis and closeToZAxis calculations
    // ... [rest of the shader code] ...


    vec3 axisColorX = vec3(0.0, 0.0, 1.0);// Blue for X axis
    vec3 axisColorZ = vec3(1.0, 0.0, 0.0);// Red for Z axis
    vec3 gridColor = vec3(1.0);// White for grid

    vec3 tmp = vec3(v_texCoord*u_gridSize, 0.0);
    vec3 worldPos = vec3(tmp.x, 0.0, tmp.y);
    // Determine proximity to X and Z axes
    bool closeToXAxis = false;
    bool closeToZAxis = false;

    if (worldPos.x < 0.25 && worldPos.x > -0.25)
    {
        closeToXAxis=true;
    }
    if (worldPos.z < 0.25 && worldPos.z > -0.25)
    {
        closeToZAxis=true;
    }

    // Calculate grid line width for antialiasing
    float lineDistX = min(abs(fract(fragPos3D.x) - 0.5), abs(fract(fragPos3D.x - 0.5)));
    float lineDistZ = min(abs(fract(fragPos3D.z) - 0.5), abs(fract(fragPos3D.z - 0.5)));
    float lineWidthX = fwidth(fragPos3D.x) * dynamicThickness;
    float lineWidthZ = fwidth(fragPos3D.z) * dynamicThickness;
    float alphaX = 1.0 - smoothstep(0.0, lineWidthX, lineDistX);
    float alphaZ = 1.0 - smoothstep(0.0, lineWidthZ, lineDistZ);

    // Color and alpha assignment
    if (closeToXAxis) {
        gl_FragColor.xyz = axisColorX;// Full alpha for axes
    } else if (closeToZAxis) {
        gl_FragColor.xyz = axisColorZ;
    }

    //    if (closeToXAxis || closeToZAxis) {
    //        gl_FragColor.a = max(alphaX, alphaZ); // Antialiasing
    ////        distance fallback
    //
    //    }


}
