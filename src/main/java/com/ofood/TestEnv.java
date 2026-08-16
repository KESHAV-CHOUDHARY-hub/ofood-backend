package com.ofood;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import com.ofood.security.jwt.RsaKeyProperties;

@Component
public class TestEnv implements CommandLineRunner {
    private final Environment env;
    private final RsaKeyProperties props;
    public TestEnv(Environment env, RsaKeyProperties props) { this.env = env; this.props = props; }
    @Override
    public void run(String... args) {
        System.out.println("ENV_VAR: " + System.getenv("OFOOD_JWT_PRIVATE_KEY_PATH"));
        System.out.println("SPRING_ENV: " + env.getProperty("OFOOD_JWT_PRIVATE_KEY_PATH"));
        System.out.println("PROPS_FIELD: " + props.getPrivateKeyPath());
    }
}
