package fr.uga.l3miage.library.authors;

import fr.uga.l3miage.data.domain.Author;
import fr.uga.l3miage.data.domain.Book;
import fr.uga.l3miage.library.books.BookDTO;
import fr.uga.l3miage.library.books.BooksMapper;
import fr.uga.l3miage.library.service.AuthorService;
import fr.uga.l3miage.library.service.BookService;
import fr.uga.l3miage.library.service.DeleteAuthorException;
import fr.uga.l3miage.library.service.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.Console;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Pattern;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
public class AuthorsController {

    private final AuthorService authorService;
    private final AuthorMapper authorMapper;
    private final BooksMapper booksMapper;
    private final BookService bookService;

    @Autowired
    public AuthorsController(AuthorService authorService, AuthorMapper authorMapper, BooksMapper booksMapper, BookService bookService) {
        this.authorService = authorService;
        this.authorMapper = authorMapper;
        this.booksMapper = booksMapper;
        this.bookService = bookService;
    }

    @GetMapping("/authors")
    public Collection<AuthorDTO> authors(@RequestParam(value = "q", required = false) String query) {
        Collection<Author> authors;
        if (query == null) {
            authors = authorService.list();
        } else {
            authors = authorService.searchByName(query);
        }
        return authors.stream()
                .map(authorMapper::entityToDTO)
                .toList();
    }

    // get an author using id
    @GetMapping("/authors/{id}")
    public AuthorDTO author( @PathVariable("id") Long id) {
        Author r=null;
        try {
             r = authorService.get(id);
        } catch (EntityNotFoundException e) {
            // TODO: recupere le status NOT_FOUND et envoie 
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"", e);
        }
        return authorMapper.entityToDTO(r);
    }
    
    
    // creat a new author
    @PostMapping("/authors")
    public AuthorDTO newAuthor(@RequestBody AuthorDTO author, HttpServletResponse response) {
        Author r=authorMapper.dtoToEntity(author);
        boolean b = Pattern.matches(" *", r.getFullName()); // true si le nom de l'auteur dans le body match le regex  " *"
        if(b){ // si le nom de l'auteur dans le body est vide ou contient plusieurs espaces
            response.setStatus(400); // status the author could not be validated
        }
        else{ // si fullName valide
            r=authorService.save(r); // utilisation du service pour sauvegarder dans la BD
            response.setStatus(201); // statut OK
        }
        return authorMapper.entityToDTO(r);
        
    }
 
    // Will update the author if found
    @PutMapping("/authors/{id}")
    public AuthorDTO updateAuthor(@RequestBody AuthorDTO author, @PathVariable Long id, HttpServletResponse response) {
        // attention AuthorDTO.id() doit être égale à id, sinon la requête utilisateur est mauvaise
        Author r=authorMapper.dtoToEntity(author);
        Author rBD=null; // auteur à chercher dans BD portant id passé en paramètre
        if(r.getId()==id){
            //public Author get(Long id) throws EntityNotFoundException
            try{
                rBD=authorService.get(id); // recherche auteur dans BD si pas trouvé leve une exception
                rBD.setFullName(r.getFullName()); // changement du nom dans l'entity
                authorService.update(rBD); // update de la BD avec le nouveau nom
            }catch(EntityNotFoundException e){
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "", e); // si auteur pas trouvé dans status 404
            }
        }
        else{
            response.setStatus(400); // status 400 si id dans le path different de l'id auteur dans le body
        }

        return authorMapper.entityToDTO(rBD);
    }

    // supression de auteur portant id de la BD si NOT_FOUND ou qu'il a des books envoie code erreur correspondant
    @DeleteMapping("/authors/{id}")
    public void deleteAuthor(@PathVariable Long id, HttpServletResponse response) {
        // unimplemented... yet!
        // public void delete(Long id) throws EntityNotFoundException, DeleteAuthorException
        try{
            authorService.delete(id);   // supression de l'auteur si trouuvé et qu'il n'a pas de books
            response.setStatus(204); // en cas de succes envoi de 204

        } catch (EntityNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"", e); // code 404 if author not found
        } catch (DeleteAuthorException de){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"", de); // code 400 if author has books
        }
    }

    // envoi au client tout les books d'un auteur portant id, si auteurpas trouvé envoi un code erreur
    @GetMapping("/authors/{authorId}/books")
    public Collection<BookDTO> books(@PathVariable Long authorId) {
        // Collection<Book> getByAuthor(Long id) throws EntityNotFoundException;
        Collection<Book> books;
        Collection<BookDTO> res;
        try{
            books=bookService.getByAuthor(authorId); // books contains all books writen by author who have the path id
            res = new HashSet<>(); // instantiation
            for(Book b : books){
                res.add(booksMapper.entityToDTO(b)); // creat a collection containing DTOs
            }
            return res;  // if OK 200 code status set by default
        }catch(EntityNotFoundException e){ // if author not found in DB set code to 404
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "", e);
        }
    }

}
