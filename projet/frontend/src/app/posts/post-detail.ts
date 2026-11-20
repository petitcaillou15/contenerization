import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { PostsService } from './posts.service';
import { AuthService } from '../core/auth.service';
import { PostDetail } from '../core/api.types';

@Component({
  selector: 'app-post-detail',
  imports: [FormsModule, DatePipe],
  templateUrl: './post-detail.html',
})
export class PostDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private posts = inject(PostsService);
  auth = inject(AuthService);

  post = signal<PostDetail | null>(null);
  id = 0;
  newComment = '';

  ngOnInit(): void {
    this.route.paramMap.subscribe(pm => {
      this.id = Number(pm.get('id'));
      this.load();
    });
  }
  load(): void { this.posts.detail(this.id).subscribe(p => this.post.set(p)); }

  addComment(): void {
    if (!this.newComment.trim()) return;
    this.posts.addComment(this.id, this.newComment).subscribe(() => {
      this.newComment = '';
      this.load();
    });
  }
  canDelete(): boolean {
    return this.auth.currentUserId() !== null
        && this.auth.currentUserId() === this.post()?.author_id;
  }
  remove(): void {
    this.posts.remove(this.id).subscribe(() => this.router.navigateByUrl('/'));
  }
}
