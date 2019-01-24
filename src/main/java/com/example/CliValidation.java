package com.example;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provides basic parameter validation for a tool.
 */
public class CliValidation {

    @Spec
    private CommandLine.Model.CommandSpec spec;

    private CliValidation() {

        // Instantiation prevention
    }

    @Option(names = "--parallelism", description = "Degree of parallelism (i.e. how many files to process simultaneously).")
    public void numberOfThreads(Integer parallelism) {
        if (parallelism <= 0) {
            throw new ParameterException(spec.commandLine(),
                    "Parallelism must be greater than 0.");
        }
    }

    @Option(required = true, names = "sourceDirectory", description = "Source directory (where original CSV files are located).")
    public void sourceDirectory(File sourceDir) throws IOException {
        final String parameterName = "Source directory";
        validateDir(sourceDir,
                parameterName);

        Files.list(sourceDir.toPath()).map(Path::toFile).filter(File::isFile)
                .filter(file -> file.length() > 0).findAny()
                .orElseThrow(() -> new ParameterException(spec.commandLine(),
                        parameterName
                                + " must contain at least one accessible non-empty file."));
    }

    @Option(required = true, names = "destinationDirectory", description = "Destination directory (where resulting CSV files will be located).")
    public void destinationDirectory(File destDir) {
        validateDir(destDir,
                "Destination directory");
    }

    private void validateDir(File potetialDir, String parameterName) {
        if (!potetialDir.isDirectory()) {
            throw new ParameterException(spec.commandLine(),
                    parameterName + " must be an existing and accessible directory.");
        }
    }

    static void validate(String[] args) {
        final CommandLine commandLine = new CommandLine(new CliValidation());
        try {
            commandLine.parse(args);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            commandLine.usage(System.err);
            throw e;
        }
    }

}
