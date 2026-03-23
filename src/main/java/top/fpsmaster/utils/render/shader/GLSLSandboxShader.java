package top.fpsmaster.utils.render.shader;

import top.fpsmaster.modules.logger.ClientLogger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL20.*;

public class GLSLSandboxShader {
    private final int programId;
    private final int timeUniform;
    private final int mouseUniform;
    private final int resolutionUniform;
    private final int animationUniform;

    public GLSLSandboxShader(String fragmentShaderLocation) throws Exception {
        int program = glCreateProgram();
        int vertexShader = createShader(GLSLSandboxShader.class.getResourceAsStream("/assets/minecraft/client/shaders/passthrough.glsl"), GL_VERTEX_SHADER);
        int fragmentShader = createShader(GLSLSandboxShader.class.getResourceAsStream("/assets/minecraft/client/shaders/" + fragmentShaderLocation), GL_FRAGMENT_SHADER);
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);

        glLinkProgram(program);

        int linked = glGetProgrami(program, GL_LINK_STATUS);

        // If linking failed
        if (linked == 0) {
            ClientLogger.error(glGetProgramInfoLog(program, glGetProgrami(program, GL_INFO_LOG_LENGTH)));
            throw new IllegalStateException("Shader failed to link");
        }
        glDetachShader(program, vertexShader);
        glDetachShader(program, fragmentShader);
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        this.programId = program;

        // Setup uniforms
        glUseProgram(program);

        this.timeUniform = glGetUniformLocation(program, "time");
        this.mouseUniform = glGetUniformLocation(program, "mouse");
        this.resolutionUniform = glGetUniformLocation(program, "resolution");
        this.animationUniform = glGetUniformLocation(program, "animation");

        glUseProgram(0);
    }

    public void useShader(int width, int height, float mouseX, float mouseY, float time, float animation) {
        glUseProgram(this.programId);

        glUniform2f(this.resolutionUniform, width, height);
        glUniform2f(this.mouseUniform, mouseX / width, 1.0f - mouseY / height);
        glUniform1f(this.timeUniform, time);
        glUniform1f(this.animationUniform, animation);
    }

    private int createShader(InputStream inputStream, int shaderType) throws IOException {
        if (inputStream == null) {
            throw new IOException("Shader resource stream is null");
        }
        int shader = glCreateShader(shaderType);
        try (InputStream stream = inputStream) {
            glShaderSource(shader, readStreamToString(stream));
        }

        glCompileShader(shader);

        int compiled = glGetShaderi(shader, GL_COMPILE_STATUS);

        // If compilation failed
        if (compiled == 0) {
            ClientLogger.error(glGetShaderInfoLog(shader, glGetShaderi(shader, GL_INFO_LOG_LENGTH)));
            throw new IllegalStateException("Failed to compile shader");
        }

        return shader;
    }

    private String readStreamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] buffer = new byte[512];

        int read;

        while ((read = inputStream.read(buffer, 0, buffer.length)) != -1) {
            out.write(buffer, 0, read);
        }

        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    public void destroy() {
        glUseProgram(0);
        glDeleteProgram(this.programId);
    }
}


