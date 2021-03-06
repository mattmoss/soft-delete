package demo

import org.springframework.http.HttpStatus

class PersonController {

    static responseFormats = ['json']

    PersonService personService

    def index() {
        respond personService.list()
    }

    def getBenAndNirav() {
        respond personService.getBenAndNirav()
    }

    def create() {
        personService.create()
        respond null, status: HttpStatus.CREATED
    }

    def delete() {
        personService.delete()
        respond null, status: HttpStatus.OK
    }
}
