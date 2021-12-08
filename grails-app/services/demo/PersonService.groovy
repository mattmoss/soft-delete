package demo

import grails.gorm.transactions.Transactional

@Transactional
class PersonService {

    def list() {
        // This does not apply the criteria adjusted by gorm-logical-delete's PreQueryListener. That listener is actually
        // called twice from the same instance:
        //      1. org.grails.datastore.mapping.query.Query.doList(), which calls...
        //      2. org.grails.orm.hibernate.query.HibernateHqlQuery.executeQuery()
        //
        // The latter calls query.list(), but that seems to make no use of the criteria in the Query.
        //      -> CriteriaQueryTypeQueryAdapter.list()
        //      -> AbstractProducedQuery<X>.list()
        //      -> .doList()
        //      -> QueryImpl.getQueryString() ... already generated without a WHERE clause. (QueryImpl is subclassed from APQ)
        //...
        // The query string (from QueryImpl.getQueryString()) appears to be pregenerated in HibernateGormStaticApi.list()
        // with the line:
        //                  Query query = session.createQuery(criteriaQuery)
        // which happens before the PreQueryEvent is published.
        def includesSoftDeleted = Person.list()

        // This is using a dynamic finder and passes thru AbstractHibernateQuery.listForCriteria, which calls criteria.list()
        // to get results (where criteria contains the conditions from gorm-logical-delete's PreQueryListener). Knowing that
        // there are no Person entities with a null name (see create method below), this does what we would expect Person.list()
        // to do.
        def doesNotIncludeSoftDeleted = Person.findAllByNameIsNotNull()
    }

    def create() {
        ['Ben', 'Nirav', 'Jeff', 'Matthew'].each {
            new Person(name: it).save(failOnError: true, flush: true)
        }
    }
    
    def delete() {
        def peopleToDelete = ['Ben', 'Jeff'].findResults { Person.findByName(it) }
        peopleToDelete.each { it.delete() }
    }
}
