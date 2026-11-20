import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { PostsService } from './posts.service';
import { AuthService } from '../core/auth.service';
import { Page, PostSummary } from '../core/api.types';

@Component({
  selector: 'app-feed',
  imports: [FormsModule, RouterLink, DatePipe],
  templateUrl: './feed.html',
})
export class FeedComponent implements OnInit {
  private posts = inject(PostsService);
  auth = inject(AuthService);

  page = signal<Page<PostSummary>>({ items: [], page: 0, limit: 20, total: 0 });
  pageIndex = 0;
  newTitle = '';
  newContent = '';

  ngOnInit(): void { this.load(); }

  load(): void {
    this.posts.feed(this.pageIndex, 20).subscribe(p => this.page.set(p));
  }
  next(): void { this.pageIndex++; this.load(); }
  prev(): void { if (this.pageIndex > 0) { this.pageIndex--; this.load(); } }

  create(): void {
    if (!this.newTitle.trim() || !this.newContent.trim()) return;
    this.posts.create(this.newTitle, this.newContent).subscribe(() => {
      this.newTitle = '';
      this.newContent = '';
      this.pageIndex = 0;
      this.load();
    });
  }
}
