import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { UsersService } from './users.service';
import { AuthService } from '../core/auth.service';
import { PublicProfile } from '../core/api.types';

@Component({
  selector: 'app-profile',
  imports: [FormsModule, DatePipe],
  templateUrl: './profile.html',
})
export class ProfileComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private users = inject(UsersService);
  private auth = inject(AuthService);

  profile = signal<PublicProfile | null>(null);
  id = 0;
  bio = '';
  saved = false;

  ngOnInit(): void {
    this.route.paramMap.subscribe(pm => {
      this.id = Number(pm.get('id'));
      this.users.profile(this.id).subscribe(p => { this.profile.set(p); this.bio = p.bio ?? ''; });
    });
  }
  isMe(): boolean { return this.auth.currentUserId() === this.id; }
  save(): void {
    this.users.updateBio(this.bio).subscribe(p => { this.profile.set(p); this.saved = true; });
  }
}
