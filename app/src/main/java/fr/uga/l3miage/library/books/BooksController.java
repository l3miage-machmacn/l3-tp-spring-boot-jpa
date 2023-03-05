package fr.uga.l3miage.library.books;

import fr.uga.l3miage.data.domain.Author;
import fr.uga.l3miage.data.domain.Book;
import fr.uga.l3miage.data.domain.Book.Language;
import fr.uga.l3miage.library.authors.AuthorDTO;
import fr.uga.l3miage.library.authors.AuthorMapper;
import fr.uga.l3miage.library.service.AuthorService;
import fr.uga.l3miage.library.service.BookService;
import fr.uga.l3miage.library.service.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.util.EnglishEnums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.service.annotation.DeleteExchange;

import java.io.IOException;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
public class BooksController {

    private final BookService bookService;
    private final BooksMapper booksMapper;
    private final AuthorService authorService;
    private final AuthorMapper authorMapper;

    @Autowired
    public BooksController(BookService bookService, BooksMapper booksMapper, AuthorService authorService,
            AuthorMapper authorMapper) {
        this.bookService = bookService;
        this.booksMapper = booksMapper;
        this.authorService = authorService;
        this.authorMapper = authorMapper;
    }

    // get all books if q parma is not specified
    // if it's specified get all books filtered by q param (case-insensitive)
    @GetMapping("/books")
    public Collection<BookDTO> books(@RequestParam(name = "q", required = false) String query) {
        Collection<Book> allBooks = bookService.list(); // collection containing all the entity books
        Collection<BookDTO> res = new HashSet<>(); // returned collection
        if (query == null) { // if q parm is defined in the Path
            for (Book b : allBooks) { // transform all existing book entities into book DTO
                res.add(booksMapper.entityToDTO(b));
            }
            return res;
        } else { // if q parm is specified look only for books having a title containning the
                 // string query
            for (Book b : allBooks) {
                if (b.getTitle().toLowerCase().contains(query.toLowerCase())) { // case-insesitive
                    res.add(booksMapper.entityToDTO(b));
                }
            }
            return res;
        }
    }

    @GetMapping("/books/{bookId}")
    public BookDTO book(@PathVariable Long bookId) {
        // public Book get(Long id) throws EntityNotFoundException
        Book b;
        try {
            b = bookService.get(bookId);
            return booksMapper.entityToDTO(b); // default code value 200 applicated
        } catch (EntityNotFoundException e) { // if book not found set code to 404
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    // creat a new book
    // !!!!!!!!!! if the same book exist it creats a new object with another id !!!!
    // !!!!!! business logic not respected in this method bookService.save(authorId,
    // newBook) !!!!
    @PostMapping("/authors/{authorId}/books")
    public BookDTO newBook(@PathVariable Long authorId, @RequestBody BookDTO book, HttpServletResponse response) {

        try {
            Book newBook = booksMapper.dtoToEntity(book); // transform a book from DTO to entity, throws an exception
            int isbnLength = String.valueOf(newBook.getIsbn()).length(); // number of isbn 's digits
            int currentYear = YearMonth.now().getYear(); // current year
            /*
             * // searching loop
             * int i = 0;
             * int languageEnumLength = Language.values().length;
             * while (i < languageEnumLength &&
             * !(newBook.getLanguage().name().equals(Language.values()[i].name()))) {
             * i++;
             * }
             */
            if (newBook.getTitle() == null) {
                throw new Exception("title not found in http query's body.");
            }
            /*
             * else if (i >= languageEnumLength) {
             * throw new Exception("Language not valid in http query's body.");
             * }
             */

            // !!!!!!! constraint on isbn is not specified
            else if (isbnLength < 10) { // isbn must have more than 10 digits
                throw new Exception("invalid Isbn in http query's body.");
            }
            // !!!!!! marge de 10 ans dans le futur pour la representabilité d'un book,
            // faute de precision spec
            else if (newBook.getYear() >= currentYear + 10) {
                throw new Exception("invalid year in http query's body.");
            } else {
                Author author = authorService.get(authorId); // get the author specified by the PathVariable
                newBook.addAuthor(author); // on ajoute l'auteur au book
                bookService.save(authorId, newBook); // saving in DB with explicit setting of (book.id) and
                                                     // (book.author) to
                                                     // book parameters
                response.setStatus(201); // if no exception thrown setStatus to 201
                return booksMapper.entityToDTO(newBook);
            }
        } catch (EntityNotFoundException e) { // if author not found status is set to 404
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "the author was not found", e);
        } catch (Exception z) { // status 400,
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, z.getMessage(), z);
        }

    }

    // !!!!! updating all the book properties except id and authors, not specified
    // in specification documents, deducted from tests
    // error in the git file, this method take bookId not authorId
    @PutMapping("books/{bookId}")
    public BookDTO updateBook(@PathVariable Long bookId, @RequestBody BookDTO book) {
        // attention BookDTO.id() doit être égale à id, sinon la requête utilisateur est
        // mauvaise
        try {
            if (bookId != book.id()) {
                throw new Exception("id not equal");
            }
            Book bookEntity = booksMapper.dtoToEntity(book); // book from body
            Book bookOld = bookService.get(bookId); // get the oldBook to update from DB, if not found throw eception
            Set<Author> oldAuthors = bookOld.getAuthors(); // authors set from bookOld
            bookEntity.setAuthors(oldAuthors);
            // bookService.addAuthor(, bookId);
            return booksMapper.entityToDTO(bookService.update(bookEntity)); // update throws an exception book not found
        } catch (EntityNotFoundException e) { // if book doesn't exist in database
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "the book was not found", e);
        } catch (Exception z) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id not equal", z);
        }

    }

    // delete the book having bookId
    // !!!!!! why in test is written (Delete second book)
    @DeleteMapping("/books/{bookId}")
    public void deleteBook(@PathVariable Long bookId, HttpServletResponse response) {
        // void delete(Long id) throws EntityNotFoundException;

        try {
            bookService.delete(bookId); // delete book from database
            response.setStatus(204); // if OK set status to 204
        } catch (EntityNotFoundException e) { // if book not found set code to 404
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "book not found", e);
        }
    }

    // add an author given in the query's body to the book indecated in the path
    // !!!!! parametre faux devrait etre bookId et non un author id
    // !!!!!!! spec not defined in openAPI deducted from test file
    // !!!!!! BAD_REQUEST instead of NOT_FOUND
    @PutMapping("/books/{bookId}/authors")
    public BookDTO addAuthor(@PathVariable Long bookId, @RequestBody AuthorDTO author, HttpServletResponse response) {

        try {
            authorService.get(author.id()); // get author from dataBase
        } catch (EntityNotFoundException e) { // if author doesn't exist in dataBase
            authorService.save(authorMapper.dtoToEntity(author)); // creat author in DB
        }

        Book bookWithAuthorBinded;
        try {
            bookWithAuthorBinded = bookService.addAuthor(bookId, author.id());
            bookWithAuthorBinded = bookService.update(bookWithAuthorBinded);
            return this.booksMapper.entityToDTO(bookWithAuthorBinded);
        } // default code 200 is set
        catch (EntityNotFoundException e) { // if book not found set code to 404
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "book not found", e);
        }
    }

}