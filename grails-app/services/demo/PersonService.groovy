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

    def getBenAndNirav() {
        // PASS
        def whereQuery = Person.where {
            name == 'Ben' || name == 'Nirav'
        }.list()
        assert whereQuery.size() == 1

        // PASS
        def findAllQuery = Person.findAll {
            name == 'Ben' || name == 'Nirav'
        }
        assert findAllQuery.size() == 1

        // Probably fails for the same reason as Person.withCriteria below.
        // FAIL
        def critQuery = (Person.createCriteria()) {
            or {
                eq 'name', 'Ben'
                eq 'name', 'Nirav'
            }
        }
        assert critQuery.size() == 2 // SHOULD == 1 thus a failure

        // criteria is built-up in AbstractHibernateCriteriaBuilder (instance: HibernateCriteriaBuilder)
        // calls `criteria.list()` with publishing PreQueryEvent/PostQueryEvent (similar: `criteria.createPagedResultList()`)
        // FAIL
        def withCriteriaQuery = Person.withCriteria {
            or {
                eq 'name', 'Ben'
                eq 'name', 'Nirav'
            }
        }
        assert withCriteriaQuery.size() == 2 // SHOULD == 1 thus a failure

        // PASS
        def dynFinderQuery = Person.findAllByNameOrName('Ben', 'Nirav')
        assert dynFinderQuery.size() == 1

        // PASS
        def withDeletedWhereQuery = Person.withDeleted {
            Person.where {
                name == 'Ben' || name == 'Nirav'
            }.list()
        }
        assert withDeletedWhereQuery.size() == 2

        // PASS
        def withDeletedDynFinderQuery = Person.withDeleted {
            Person.findAllByNameOrName('Ben', 'Nirav')
        }
        assert withDeletedWhereQuery.size() == 2

        withDeletedDynFinderQuery
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
