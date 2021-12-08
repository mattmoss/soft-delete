package demo

import gorm.logical.delete.LogicalDelete
import gorm.logical.delete.typetrait.StringLogicalDelete

class Person implements StringLogicalDelete<Person> {

    String name

    static constraints = {
        deleted nullable: true
    }
}
