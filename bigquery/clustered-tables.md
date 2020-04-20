### Clustered Table

Clustered tables only applicable for `PARTITIONED` tables. Another limitation also mentioned in [there](https://cloud.google.com/bigquery/docs/creating-clustered-tables#limitations).
Table clustering is possible for tables partitioned by:
 
 - ingestion time
 - date/timestamp
 - integer range
                                                         
Currently, BigQuery allows clustering over a partitioned table. Use clustering over a partitioned table when:

- Your data is already partitioned on a date, timestamp, or integer column.
- You commonly use filters or aggregation against particular columns in your queries.

More bench marking about BigQuery clustered tables already posted in [there](https://medium.com/google-cloud/bigquery-optimized-cluster-your-tables-65e2f684594b).

### Examples

#### Creating a Table Based on YAML Schema

Here's an example of using a `google-cloud-bigquery` client for `Java/Kotlin`.

I create a new `gradle` project using IntelliJ with the following `gradle.properties`:

```aidl
kotlin.code.style=official
kotlinVersion = 1.3.21
# Set to 1.54.0 based on https://github.com/googleapis/google-cloud-java/tree/v0.72.0/google-cloud-clients/google-cloud-bigquery
# Simply to have this patch https://github.com/googleapis/google-cloud-java/pull/3984
bigqueryVersion = 1.98.0
```

And then I create edit the `build.gradle`:
```aidl
buildscript {
    repositories {
        mavenCentral()
        jcenter()
    }
    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
        classpath 'com.github.jengelman.gradle.plugins:shadow:2.0.2'
    }
}

plugins {
    id 'java'
    id 'org.jetbrains.kotlin.jvm' version '1.3.61'
}

group 'com.github.irvifa'
version '1.0-SNAPSHOT'

apply plugin: 'java'
apply plugin: 'idea'
apply plugin: 'kotlin'
apply plugin: 'com.github.johnrengelman.shadow'
sourceCompatibility = 1.8

ext {
    artifactId = 'bigquery-schema-loader'
    mainClassName = "com.github.irvifa.gcp.examples.bigquery.schemaloader.ExpBigQuerySchemaLoader"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8"
    compile "com.google.cloud:google-cloud-bigquery:$bigqueryVersion"
    compile 'com.fasterxml.jackson.core:jackson-databind:2.7.1-1'
    compile 'com.fasterxml.jackson.module:jackson-module-kotlin:2.7.1-2'
    compile 'com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.7.1'
    testCompile group: 'junit', name: 'junit', version: '4.12'
}

compileKotlin {
    kotlinOptions.jvmTarget = "1.8"
}
compileTestKotlin {
    kotlinOptions.jvmTarget = "1.8"
}

shadowJar {
    classifier = null
    mergeServiceFiles()
    exclude 'META-INF/*.DSA', 'META-INF/*.RSA', 'META-INF/*.SF'
    manifest {
        attributes 'Main-Class': mainClassName
    }
}
```


```aidl
package com.github.irvifa.gcp.examples.bigquery.schemaloader.api

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class Field(
    val name: String,
    val type: String
) : Serializable

data class ClusteringColumn(val name: String) : Serializable

data class BigQuerySchema(
    @JsonProperty("table_name") val tableName: String,
    val fields: List<Field>,
    @JsonProperty("clustering_column_list") val clusteringColumns: List<ClusteringColumn>
) : Serializable
```

```aidl
package com.github.irvifa.gcp.examples.bigquery.schemaloader.exceptions

class DataTypeNotFoundException (targetDataType: String) :
    Exception("Data type not found for: $targetDataType")
```

```aidl
package com.github.irvifa.gcp.examples.bigquery.schemaloader.util

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.Schema
import com.github.irvifa.gcp.examples.bigquery.schemaloader.api.BigQuerySchema
import com.github.irvifa.gcp.examples.bigquery.exceptions.DataTypeNotFoundException

class BigQuerySchemaLoaderUtil {
    fun getBigQuerySchema(bigQuerySchema: BigQuerySchema): Schema {
        val fields: MutableList<Field> = mutableListOf()
        bigQuerySchema.fields.forEach{field -> fields.add(Field.of(field.name, convertDataTypeToStandardSql(field.type)))}
        return Schema.of(fields)
    }

    fun getClusteringColumnName(bigQuerySchema: BigQuerySchema): MutableList<String> {
        val clusteringColumns = mutableListOf<String>()
        bigQuerySchema.clusteringColumns.forEach{ column -> clusteringColumns.add(column.name) }
        return clusteringColumns
    }

    private fun convertDataTypeToStandardSql(targetType: String): LegacySQLTypeName {
        return when (targetType) {
            "STRING" -> LegacySQLTypeName.STRING
            "INTEGER" -> LegacySQLTypeName.INTEGER
            "TIMESTAMP" -> LegacySQLTypeName.TIMESTAMP
            "DATETIME" -> LegacySQLTypeName.DATETIME
            else -> throw DataTypeNotFoundException(targetType)
        }
    }
}
```

And then create a main class:

```aidl
package com.github.irvifa.gcp.examples.bigquery.schemaloader

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.Clustering
import com.google.cloud.bigquery.StandardTableDefinition
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
import com.google.cloud.bigquery.TimePartitioning
import com.github.irvifa.gcp.examples.bigquery.schemaloader.api.BigQuerySchema
import com.github.irvifa.gcp.examples.bigquery.schemaloader.util.BigQuerySchemaLoaderUtil

class BigQuerySchemaLoader {
    companion object {
        private val parser = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())
        private val loader = BigQuerySchemaLoaderUtil()

        // http://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#static-methods
        @JvmStatic
        fun main(args: Array<String>) {
            val projectId = args[0]
            val datasetName = args[1]
            val schemaPath = args[2]
            println(args[0])

            val bigquery = BigQueryOptions.newBuilder().setProjectId(projectId).build().service
            val tableInfo = getTableInfo(datasetName, schemaPath)
            createTable(bigquery, tableInfo)
        }

        private fun loadBigQuerySchemaFromResource(url: String): BigQuerySchema {
            val resource = Thread.currentThread().contextClassLoader.getResource(url)
            val bigquerySchema: BigQuerySchema = parser.readValue(
            resource,
            object : TypeReference<BigQuerySchema>() {})

            return bigquerySchema
        }

        // https://cloud.google.com/bigquery/docs/creating-clustered-tables#creating_an_empty_clustered_table_with_a_schema_definition
        private fun getTableInfo(datasetName: String, schemaName: String): TableInfo {
            val bigQuerySchema = loadBigQuerySchemaFromResource(schemaName)

            val tableId = TableId.of(datasetName, bigQuerySchema.tableName)
            val partitioning: TimePartitioning = TimePartitioning.of(TimePartitioning.Type.DAY)
            val schema = loader.getBigQuerySchema(bigQuerySchema)
            val clusteringColumns = loader.getClusteringColumnName(bigQuerySchema)
            val clustering: Clustering =
                Clustering.newBuilder().setFields(clusteringColumns).build()
            val tableDefinition = StandardTableDefinition.newBuilder()
                .setSchema(schema)
                .setTimePartitioning(partitioning)
                .setClustering(clustering)
                .build()
           return TableInfo.newBuilder(tableId, tableDefinition).build()
        }

        private fun createTable(bigquery: BigQuery, tableInfo: TableInfo) {
            bigquery.create(tableInfo)
        }
    }
}
```

In the `resources` directory I put the following YAML:

```aidl
table_name: example

fields:
  - name: name
    type: STRING
```

To use, we can do the following command:

```aidl
./gradlew shadowJar
export GOOGLE_APPLICATION_CREDENTIALS=<path-to-svc-acc>
java -jar build/libs/bigquery-schema-loader-1.0-SNAPSHOT.jar <project-id> <dataset-name> <schema-file>
```
