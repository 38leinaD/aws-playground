package de.dplatz;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tags;

public class CDKApp {
    
    public static void main(final String[] args) {
            var app = new App();
            var appName = "aws-playground";
            Tags.of(app).add("project", "aws-playground");

            var stackProps = StackProps.builder()
                    .env(Environment.builder()
                            .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                            .region(System.getenv("CDK_DEFAULT_REGION"))
                            .build())
                    .build();
        new CDKStack(app, appName, stackProps);
        app.synth();
    }
}
