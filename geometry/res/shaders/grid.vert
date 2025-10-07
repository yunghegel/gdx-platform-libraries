#version 150

in vec3 a_position;
in vec2 a_texCoord;
in vec4 a_color;

uniform mat4 u_projTrans;
uniform mat4 u_viewTrans;

out vec2 v_texCoord;
out vec2 v_camPos;
out vec4 v_color;
out vec3 v_farPoint;
out vec3 v_nearPoint;
out vec3 v_position;
out vec3 v_eyePos;

uniform float u_gridSize;

const vec3 pos[4] = vec3[4](
vec3(-1.0, 0.0, -1.0),
vec3(1.0, 0.0, -1.0),
vec3(1.0, 0.0, 1.0),
vec3(-1.0, 0.0, 1.0)
);

const int indices[6] = int[6](
0, 1, 2, 2, 3, 0
);

const vec3 gridPlane[4] = vec3[] (
vec3(-1.0, -1.0, 0.0), vec3(1.0, -1.0, 0), vec3(1.0, 1.0, 0.0),
vec3(-1.0, 1.0, 0.0)
);

vec3 UnprojectPoint(float x, float y, float z, mat4 view, mat4 projection) {
    mat4 viewInv = inverse(view);
    mat4 projInv = inverse(projection);
    vec4 unprojectedPoint =  viewInv * projInv * vec4(x, y, z, 1.0);
    return unprojectedPoint.xyz / unprojectedPoint.w;
}

void main(){
    ////    extract camera position from view matrix
    mat4 inverseViewMatrix = inverse(u_viewTrans);
    vec3 camPos =  vec3(inverseViewMatrix[3]);
    v_eyePos = camPos;


    mat4 MVP = u_projTrans * u_viewTrans;
    int idx = indices[gl_VertexID];
    vec3 position = a_position * u_gridSize;


    position.x += camPos.x;
    position.z += camPos.z;

    v_camPos = camPos.xz;
    v_position = position;

    gl_Position = MVP * vec4(position, 1.0);
    v_texCoord = position.xz;
    v_color = a_color;
    //int idx = mod(gl_VertexID, 6.0);
    //    int idx = int(mod(gl_VertexID,6));
    //    vec3 position = a_position * u_gridSize;
    //vec3 p = gridPlane[idx];
    //v_nearPoint = UnprojectPoint(p.x, p.y, 0.0, u_viewTrans, u_projTrans).xyz; // unprojecting on the near plane
    //v_farPoint = UnprojectPoint(p.x, p.y, 1.0, u_viewTrans, u_projTrans).xyz; // unprojecting on the far plane
    //gl_Position = vec4(p, 1.0); // using directly the clipped coordinates
}