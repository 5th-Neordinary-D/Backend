package ne.ordinary.dd.repository;

import ne.ordinary.dd.domain.Comment;
import ne.ordinary.dd.domain.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    @Query("SELECT cl " +
            "from CommentLike cl " +
            "where cl.comment.commentId = :commentId ")
    Optional<CommentLike> findCommentLikeByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT cl " +
            "from CommentLike cl " +
            "where cl.user.id = :userId and cl.comment.commentId = :commentId ")
    Optional<CommentLike> findCommentLikeByUserCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);

    void delete(CommentLike commentLike);

    @Query("select count(*) " +
            "from CommentLike cl " +
            "where cl.comment.commentId = :commentId and cl.type = :type")
    int countByCommentIdAndType(@Param("commentId") Long commentId, @Param("type") int type);

    @Query("select count(*) " +
            "from CommentLike cl " +
            "where cl.comment.commentId = :commentId and cl.user.id = :userId")
    int countByCommentIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Modifying
    @Query("delete from CommentLike cl " +
            "where cl.comment.feed.id = :feedId")
    void deleteByFeedId(@Param("feedId") Long id);
}
