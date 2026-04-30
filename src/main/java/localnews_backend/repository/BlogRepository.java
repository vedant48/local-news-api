package localnews_backend.repository;

import localnews_backend.model.Blog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BlogRepository extends MongoRepository<Blog, String> {
    List<Blog> findByLocation(String location);
}