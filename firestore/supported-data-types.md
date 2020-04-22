### Supported Data Types

Currently Set is not available as part of [supported data type](https://firebase.google.com/docs/firestore/manage-data/data-types), 
it seems like this is linked to the fact that the implementation of `Set` is tightly coupled
with Protobuf since I notice that `MapValue` and `ArrayValue` in Firestore is using `protocolbuffer`
in the implementation and then I saw this issues about set implementation in `protobuf`. 

1. https://github.com/protocolbuffers/protobuf/issues/3359
2. https://github.com/protocolbuffers/protobuf/issues/3432

See [this issue](https://github.com/googleapis/java-firestore/issues/189) 
if you want to know about the feature request.

In the meantime you can convert `Set` to `ArrayList` when put/get the data from `Firestore`.

Example:

```aidl
package com.github.irvifa.gcp.examples.firestore.repository

import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.FirestoreOptions
import com.google.gson.Gson
import com.github.irvifa.gcp.examples.firestore.api.Person
import org.apache.hadoop.hbase.client.ResultScanner
import org.apache.hadoop.hbase.client.Scan
import org.slf4j.LoggerFactory
import java.io.IOException

class PersonRepository(builder: Builder){
    private val gson: Gson
    private val table: CollectionReference

    init {
        this.gson = Gson()
        this.table = connectToFirestoreTable(builder.projectId, builder.tableName)
    }

    private fun connectToFirestoreTable(projectId: String, tableName: String):
        CollectionReference {
        val firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
            .setProjectId(projectId)
            .build()
        val db = firestoreOptions.getService()
        return db.collection(tableName)
    }

    fun putPerson(name: String, person: Person) {
        this.table.document(name).set(person)
    }

    fun getPerson(name: String): Person? {
        val docReference = this.table.document(name)
        val future = docReference.get()
        val document = future.get()

        return if (document.exists()) {
            return document.toObject(Person::class.java)
        } else null
    }

    class Builder(projectId: String, tableName: String) {
        internal val projectId: String = checkNotNull(projectId)
        internal val tableName: String = checkNotNull(tableName)

        internal var privateKeyValue: String? = null

        fun setPrivateKeyValue(privateKeyValue: String): Builder {
            this.privateKeyValue = checkNotNull(privateKeyValue)
            return this
        }

        fun build(): PersonRepository {
            return PersonRepository(this)
        }
    }

    companion object {
        private val GSON = Gson()
        private val LOGGER = LoggerFactory.getLogger(PersonRepository::class.java)
    }
}
```

For more information about best practices you can see it in [there](https://firebase.google.com/docs/firestore/best-practices).
