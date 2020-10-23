/*
 * Copyright (C) 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.syndesis.integration.project.generator.mvn;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import io.syndesis.common.util.MavenProperties;

public final class PomContext {
    private final String artifactId;
    private final String name;
    private final String description;
    private final Collection<MavenGav> dependencies;
    private final MavenProperties mavenProperties;

    public PomContext(String artifactId, String name, String description, Collection<MavenGav> dependencies, MavenProperties mavenProperties) {
        this.artifactId = artifactId;
        this.name = name;
        this.description = description;
        this.dependencies = dependencies;
        this.mavenProperties = mavenProperties;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Collection<MavenGav> getDependencies() {
        return dependencies;
    }

    public Set<Map.Entry<String, String>> getMavenRepositories() {
        return mavenProperties.getRepositories().entrySet();
    }
}
